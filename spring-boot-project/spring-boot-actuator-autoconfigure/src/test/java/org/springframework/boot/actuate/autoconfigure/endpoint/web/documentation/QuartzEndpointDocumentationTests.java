/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.endpoint.web.documentation;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.quartz.CalendarIntervalScheduleBuilder;
import org.quartz.CalendarIntervalTrigger;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.DailyTimeIntervalScheduleBuilder;
import org.quartz.DailyTimeIntervalTrigger;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.TimeOfDay;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.GroupMatcher;

import org.springframework.boot.actuate.quartz.QuartzEndpoint;
import org.springframework.boot.actuate.quartz.QuartzEndpointWebExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.replacePattern;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for generating documentation describing the {@link QuartzEndpoint}.
 *
 * @author Vedran Pavic
 */
class QuartzEndpointDocumentationTests extends MockMvcEndpointDocumentationTests {

	private static final JobDetail jobOne = JobBuilder.newJob(SampleJob.class).withIdentity("jobOne", "samples")
			.withDescription("A sample job").build();

	private static final JobDetail jobTwo = JobBuilder.newJob(Job.class).withIdentity("jobTwo", "samples").build();

	private static final JobDetail jobThree = JobBuilder.newJob(Job.class).withIdentity("jobThree", "tests").build();

	private static final CronTrigger triggerOne = TriggerBuilder.newTrigger().forJob(jobOne)
			.withDescription("3AM on weekdays").withIdentity("3am-weekdays", "samples")
			.withSchedule(CronScheduleBuilder.atHourAndMinuteOnGivenDaysOfWeek(3, 0, 1, 2, 3, 4, 5)).build();

	private static final SimpleTrigger triggerTwo = TriggerBuilder.newTrigger().forJob(jobOne)
			.withDescription("Once a day").withIdentity("every-day", "samples")
			.withSchedule(SimpleScheduleBuilder.repeatHourlyForever(24)).build();

	private static final CalendarIntervalTrigger triggerThree = TriggerBuilder.newTrigger().forJob(jobTwo)
			.withDescription("Once a week").withIdentity("once-a-week", "samples")
			.withSchedule(CalendarIntervalScheduleBuilder.calendarIntervalSchedule().withIntervalInWeeks(1)).build();

	private static final DailyTimeIntervalTrigger triggerFour = TriggerBuilder.newTrigger().forJob(jobThree)
			.withDescription("Every hour between 9AM and 6PM on Tuesday and Thursday")
			.withIdentity("every-hour-tue-thu", "tests")
			.withSchedule(DailyTimeIntervalScheduleBuilder.dailyTimeIntervalSchedule()
					.onDaysOfTheWeek(Calendar.TUESDAY, Calendar.THURSDAY)
					.startingDailyAt(TimeOfDay.hourAndMinuteOfDay(9, 0))
					.endingDailyAt(TimeOfDay.hourAndMinuteOfDay(18, 0)).withInterval(1, IntervalUnit.HOUR))
			.build();

	@MockBean
	private Scheduler scheduler;

	@Test
	void quartzReport() throws Exception {
		String groupOne = jobOne.getKey().getGroup();
		String groupTwo = jobThree.getKey().getGroup();
		given(this.scheduler.getJobGroupNames()).willReturn(Arrays.asList(groupOne, groupTwo));
		given(this.scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupOne)))
				.willReturn(new HashSet<>(Arrays.asList(jobOne.getKey(), jobTwo.getKey())));
		given(this.scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupTwo)))
				.willReturn(Collections.singleton(jobThree.getKey()));
		this.mockMvc.perform(get("/actuator/quartz")).andExpect(status().isOk()).andDo(document("quartz/report"));
	}

	@Test
	void quartzJob() throws Exception {
		JobKey jobKey = jobOne.getKey();
		given(this.scheduler.getJobDetail(jobKey)).willReturn(jobOne);
		given(this.scheduler.getTriggersOfJob(jobKey))
				.willAnswer((invocation) -> Arrays.asList(triggerOne, triggerTwo));
		this.mockMvc.perform(get("/actuator/quartz/jobs/groupOne/jobOne")).andExpect(status().isOk()).andDo(document(
				"quartz/job",
				preprocessResponse(replacePattern(Pattern.compile("org.quartz.Job"), "com.example.MyJob")),
				responseFields(fieldWithPath("group").description("Job group."),
						fieldWithPath("name").description("Job name."),
						fieldWithPath("description").description("Job description, if any.").optional(),
						fieldWithPath("className").description("Job class."),
						fieldWithPath("triggers.[].group").description("Trigger group."),
						fieldWithPath("triggers.[].name").description("Trigger name."),
						fieldWithPath("triggers.[].description").description("Trigger description, if any.").optional(),
						fieldWithPath("triggers.[].calendarName").description("Trigger's calendar name, if any.")
								.optional(),
						fieldWithPath("triggers.[].startTime").description("Trigger's start time."),
						fieldWithPath("triggers.[].endTime").description("Trigger's end time."),
						fieldWithPath("triggers.[].nextFireTime").type(Date.class)
								.description("Trigger's next fire time, if any.").optional(),
						fieldWithPath("triggers.[].previousFireTime").type(Date.class)
								.description("Trigger's previous fire time, if any.").optional(),
						fieldWithPath("triggers.[].finalFireTime").type(Date.class)
								.description("Trigger's final fire time, if any.").optional())));
	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseDocumentationConfiguration.class)
	static class TestConfiguration {

		@Bean
		QuartzEndpoint endpoint(Scheduler scheduler) {
			return new QuartzEndpoint(scheduler);
		}

		@Bean
		QuartzEndpointWebExtension endpointWebExtension(QuartzEndpoint endpoint) {
			return new QuartzEndpointWebExtension(endpoint);
		}

	}

	private static class SampleJob implements Job {

		@Override
		public void execute(JobExecutionContext context) {

		}

	}

}
