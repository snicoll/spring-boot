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

package org.springframework.boot.actuate.quartz;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.api.MapAssert;
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
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.TimeOfDay;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.OperableTrigger;

import org.springframework.boot.actuate.quartz.QuartzEndpoint.QuartzJobDetails;
import org.springframework.boot.actuate.quartz.QuartzEndpoint.QuartzJobGroupSummary;
import org.springframework.boot.actuate.quartz.QuartzEndpoint.QuartzJobSummary;
import org.springframework.boot.actuate.quartz.QuartzEndpoint.QuartzReport;
import org.springframework.boot.actuate.quartz.QuartzEndpoint.QuartzTriggerGroupSummary;
import org.springframework.scheduling.quartz.DelegatingJob;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link QuartzEndpoint}.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 */
class QuartzEndpointTests {

	private static final JobDetail jobOne = JobBuilder.newJob(Job.class).withIdentity("jobOne").build();

	private static final JobDetail jobTwo = JobBuilder.newJob(DelegatingJob.class).withIdentity("jobTwo").build();

	private static final JobDetail jobThree = JobBuilder.newJob(Job.class).withIdentity("jobThree", "samples").build();

	private static final Trigger triggerOne = TriggerBuilder.newTrigger().forJob(jobOne).withIdentity("triggerOne")
			.build();

	private static final Trigger triggerTwo = TriggerBuilder.newTrigger().forJob(jobOne).withIdentity("triggerTwo")
			.build();

	private static final Trigger triggerThree = TriggerBuilder.newTrigger().forJob(jobThree)
			.withIdentity("triggerThree", "samples").build();

	private final Scheduler scheduler;

	private final QuartzEndpoint endpoint;

	QuartzEndpointTests() {
		this.scheduler = mock(Scheduler.class);
		this.endpoint = new QuartzEndpoint(this.scheduler);
	}

	@Test
	void quartzReport() throws SchedulerException {
		given(this.scheduler.getJobGroupNames()).willReturn(Arrays.asList("jobSamples", "DEFAULT"));
		given(this.scheduler.getTriggerGroupNames()).willReturn(Collections.singletonList("triggerSamples"));
		QuartzReport quartzReport = this.endpoint.quartzReport();
		assertThat(quartzReport.getJobs().getGroups()).containsOnly("jobSamples", "DEFAULT");
		assertThat(quartzReport.getTriggers().getGroups()).containsOnly("triggerSamples");
		verify(this.scheduler).getJobGroupNames();
		verify(this.scheduler).getTriggerGroupNames();
		verifyNoMoreInteractions(this.scheduler);
	}

	@Test
	void quartzReportWithNoJob() throws SchedulerException {
		given(this.scheduler.getJobGroupNames()).willReturn(Collections.emptyList());
		given(this.scheduler.getTriggerGroupNames()).willReturn(Arrays.asList("triggerSamples", "DEFAULT"));
		QuartzReport quartzReport = this.endpoint.quartzReport();
		assertThat(quartzReport.getJobs().getGroups()).isEmpty();
		assertThat(quartzReport.getTriggers().getGroups()).containsOnly("triggerSamples", "DEFAULT");
	}

	@Test
	void quartzReportWithNoTrigger() throws SchedulerException {
		given(this.scheduler.getJobGroupNames()).willReturn(Collections.singletonList("jobSamples"));
		given(this.scheduler.getTriggerGroupNames()).willReturn(Collections.emptyList());
		QuartzReport quartzReport = this.endpoint.quartzReport();
		assertThat(quartzReport.getJobs().getGroups()).containsOnly("jobSamples");
		assertThat(quartzReport.getTriggers().getGroups()).isEmpty();
	}

	@Test
	void quartzJobGroupsWithExistingGroups() throws SchedulerException {
		mockJobs(jobOne, jobTwo, jobThree);
		Map<String, Object> jobGroups = this.endpoint.quartzJobGroups();
		assertThat(jobGroups).containsOnlyKeys("DEFAULT", "samples");
		assertThat(jobGroups).extractingByKey("DEFAULT", nestedMap())
				.containsOnly(entry("jobs", Arrays.asList("jobOne", "jobTwo")));
		assertThat(jobGroups).extractingByKey("samples", nestedMap())
				.containsOnly(entry("jobs", Collections.singletonList("jobThree")));
	}

	@Test
	void quartzJobGroupsWithNoGroup() throws SchedulerException {
		given(this.scheduler.getJobGroupNames()).willReturn(Collections.emptyList());
		Map<String, Object> jobGroups = this.endpoint.quartzJobGroups();
		assertThat(jobGroups).isEmpty();
	}

	@Test
	void quartzTriggerGroupsWithExistingGroups() throws SchedulerException {
		mockTriggers(triggerOne, triggerTwo, triggerThree);
		given(this.scheduler.getPausedTriggerGroups()).willReturn(Collections.singleton("samples"));
		Map<String, Object> triggerGroups = this.endpoint.quartzTriggerGroups();
		assertThat(triggerGroups).containsOnlyKeys("DEFAULT", "samples");
		assertThat(triggerGroups).extractingByKey("DEFAULT", nestedMap()).containsOnly(entry("paused", false),
				entry("triggers", Arrays.asList("triggerOne", "triggerTwo")));
		assertThat(triggerGroups).extractingByKey("samples", nestedMap()).containsOnly(entry("paused", true),
				entry("triggers", Collections.singletonList("triggerThree")));
	}

	@Test
	void quartzTriggerGroupsWithNoGroup() throws SchedulerException {
		given(this.scheduler.getTriggerGroupNames()).willReturn(Collections.emptyList());
		Map<String, Object> triggerGroups = this.endpoint.quartzTriggerGroups();
		assertThat(triggerGroups).isEmpty();
	}

	@Test
	void quartzJobGroupSummaryWithInvalidGroup() throws SchedulerException {
		given(this.scheduler.getJobGroupNames()).willReturn(Collections.singletonList("DEFAULT"));
		QuartzJobGroupSummary summary = this.endpoint.quartzJobGroupSummary("unknown");
		assertThat(summary).isNull();
	}

	@Test
	void quartzJobGroupSummaryWithEmptyGroup() throws SchedulerException {
		given(this.scheduler.getJobGroupNames()).willReturn(Collections.singletonList("samples"));
		given(this.scheduler.getJobKeys(GroupMatcher.jobGroupEquals("samples"))).willReturn(Collections.emptySet());
		QuartzJobGroupSummary summary = this.endpoint.quartzJobGroupSummary("samples");
		assertThat(summary).isNotNull();
		assertThat(summary.getName()).isEqualTo("samples");
		assertThat(summary.getJobs()).isEmpty();
	}

	@Test
	void quartzJobGroupSummaryWithJobs() throws SchedulerException {
		mockJobs(jobOne, jobTwo);
		QuartzJobGroupSummary summary = this.endpoint.quartzJobGroupSummary("DEFAULT");
		assertThat(summary).isNotNull();
		assertThat(summary.getName()).isEqualTo("DEFAULT");
		Map<String, QuartzJobSummary> jobSummaries = summary.getJobs();
		assertThat(jobSummaries).containsOnlyKeys("jobOne", "jobTwo");
		assertThat(jobSummaries.get("jobOne").getClassName()).isEqualTo(Job.class.getName());
		assertThat(jobSummaries.get("jobTwo").getClassName()).isEqualTo(DelegatingJob.class.getName());
	}

	@Test
	void quartzTriggerGroupSummaryWithInvalidGroup() throws SchedulerException {
		given(this.scheduler.getTriggerGroupNames()).willReturn(Collections.singletonList("DEFAULT"));
		QuartzTriggerGroupSummary summary = this.endpoint.quartzTriggerGroupSummary("unknown");
		assertThat(summary).isNull();
	}

	@Test
	void quartzTriggerGroupSummaryWithEmptyGroup() throws SchedulerException {
		given(this.scheduler.getTriggerGroupNames()).willReturn(Collections.singletonList("samples"));
		given(this.scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals("samples")))
				.willReturn(Collections.emptySet());
		QuartzTriggerGroupSummary summary = this.endpoint.quartzTriggerGroupSummary("samples");
		assertThat(summary).isNotNull();
		assertThat(summary.getName()).isEqualTo("samples");
		assertThat(summary.isPaused()).isFalse();
		assertThat(summary.getTriggers().getCron()).isEmpty();
		assertThat(summary.getTriggers().getSimple()).isEmpty();
		assertThat(summary.getTriggers().getDailyTimeInterval()).isEmpty();
		assertThat(summary.getTriggers().getCalendarTimeInterval()).isEmpty();
		assertThat(summary.getTriggers().getCustom()).isEmpty();
	}

	@Test
	void quartzTriggerGroupSummaryWithCronTrigger() throws SchedulerException {
		CronTrigger cronTrigger = TriggerBuilder.newTrigger().withIdentity("3am-every-day", "samples")
				.withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(3, 0)).build();
		mockTriggers(cronTrigger);
		QuartzTriggerGroupSummary summary = this.endpoint.quartzTriggerGroupSummary("samples");
		assertThat(summary.getName()).isEqualTo("samples");
		assertThat(summary.isPaused()).isFalse();
		assertThat(summary.getTriggers().getCron()).containsOnlyKeys("3am-every-day");
		assertThat(summary.getTriggers().getSimple()).isEmpty();
		assertThat(summary.getTriggers().getDailyTimeInterval()).isEmpty();
		assertThat(summary.getTriggers().getCalendarTimeInterval()).isEmpty();
		assertThat(summary.getTriggers().getCustom()).isEmpty();
	}

	@Test
	void quartzTriggerGroupSummaryWithCronTriggerDetails() throws SchedulerException {
		Date previousFireTime = Date.from(Instant.parse("2020-11-30T03:00:00Z"));
		Date nextFireTime = Date.from(Instant.parse("2020-12-01T03:00:00Z"));
		TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");
		CronTrigger cronTrigger = TriggerBuilder.newTrigger().withIdentity("3am-every-day", "samples").withPriority(3)
				.withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(3, 0).inTimeZone(timeZone)).build();
		((OperableTrigger) cronTrigger).setPreviousFireTime(previousFireTime);
		((OperableTrigger) cronTrigger).setNextFireTime(nextFireTime);
		mockTriggers(cronTrigger);
		QuartzTriggerGroupSummary summary = this.endpoint.quartzTriggerGroupSummary("samples");
		Map<String, Object> triggers = summary.getTriggers().getCron();
		assertThat(triggers).containsOnlyKeys("3am-every-day");
		assertThat(triggers).extractingByKey("3am-every-day", nestedMap()).containsOnly(
				entry("previousFireTime", previousFireTime), entry("nextFireTime", nextFireTime), entry("priority", 3),
				entry("expression", "0 0 3 ? * *"), entry("timeZone", timeZone));
	}

	@Test
	void quartzTriggerGroupSummaryWithCronTriggerOrderAccordingFireTimeAndPriority() throws SchedulerException {
		Date nextFireTime = Date.from(Instant.parse("2020-12-01T03:00:00Z"));
		TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");
		CronTrigger cronTriggerOne = TriggerBuilder.newTrigger().withIdentity("3am-weekdays", "samples").withPriority(6)
				.withSchedule(
						CronScheduleBuilder.atHourAndMinuteOnGivenDaysOfWeek(3, 0, 1, 2, 3, 4, 5).inTimeZone(timeZone))
				.build();
		((OperableTrigger) cronTriggerOne).setNextFireTime(nextFireTime);
		CronTrigger cronTriggerTwo = TriggerBuilder.newTrigger().withIdentity("3am-every-day", "samples")
				.withPriority(3).withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(3, 0).inTimeZone(timeZone))
				.build();
		((OperableTrigger) cronTriggerTwo).setNextFireTime(nextFireTime);
		mockTriggers(cronTriggerOne, cronTriggerTwo);
		QuartzTriggerGroupSummary summary = this.endpoint.quartzTriggerGroupSummary("samples");
		assertThat(summary.getTriggers().getCron().keySet()).containsExactly("3am-every-day", "3am-weekdays");
	}

	@Test
	void quartzTriggerGroupSummaryWithSimpleTrigger() throws SchedulerException {
		SimpleTrigger simpleTrigger = TriggerBuilder.newTrigger().withIdentity("every-hour", "samples")
				.withSchedule(SimpleScheduleBuilder.repeatHourlyForever(1)).build();
		mockTriggers(simpleTrigger);
		QuartzTriggerGroupSummary summary = this.endpoint.quartzTriggerGroupSummary("samples");
		assertThat(summary.getName()).isEqualTo("samples");
		assertThat(summary.isPaused()).isFalse();
		assertThat(summary.getTriggers().getCron()).isEmpty();
		assertThat(summary.getTriggers().getSimple()).containsOnlyKeys("every-hour");
		assertThat(summary.getTriggers().getDailyTimeInterval()).isEmpty();
		assertThat(summary.getTriggers().getCalendarTimeInterval()).isEmpty();
		assertThat(summary.getTriggers().getCustom()).isEmpty();
	}

	@Test
	void quartzTriggerGroupSummaryWithSimpleTriggerDetails() throws SchedulerException {
		Date previousFireTime = Date.from(Instant.parse("2020-11-30T03:00:00Z"));
		Date nextFireTime = Date.from(Instant.parse("2020-12-01T03:00:00Z"));
		SimpleTrigger simpleTrigger = TriggerBuilder.newTrigger().withIdentity("every-hour", "samples").withPriority(7)
				.withSchedule(SimpleScheduleBuilder.repeatHourlyForever(1)).build();
		((OperableTrigger) simpleTrigger).setPreviousFireTime(previousFireTime);
		((OperableTrigger) simpleTrigger).setNextFireTime(nextFireTime);
		mockTriggers(simpleTrigger);
		QuartzTriggerGroupSummary summary = this.endpoint.quartzTriggerGroupSummary("samples");
		Map<String, Object> triggers = summary.getTriggers().getSimple();
		assertThat(triggers).containsOnlyKeys("every-hour");
		assertThat(triggers).extractingByKey("every-hour", nestedMap()).containsOnly(
				entry("previousFireTime", previousFireTime), entry("nextFireTime", nextFireTime), entry("priority", 7),
				entry("interval", 3600000L));
	}

	@Test
	void quartzTriggerGroupSummaryWithDailyIntervalTrigger() throws SchedulerException {
		DailyTimeIntervalTrigger trigger = TriggerBuilder.newTrigger().withIdentity("every-hour-9am", "samples")
				.withSchedule(DailyTimeIntervalScheduleBuilder.dailyTimeIntervalSchedule()
						.startingDailyAt(TimeOfDay.hourAndMinuteOfDay(9, 0)).withInterval(1, IntervalUnit.HOUR))
				.build();
		mockTriggers(trigger);
		QuartzTriggerGroupSummary summary = this.endpoint.quartzTriggerGroupSummary("samples");
		assertThat(summary.getName()).isEqualTo("samples");
		assertThat(summary.isPaused()).isFalse();
		assertThat(summary.getTriggers().getCron()).isEmpty();
		assertThat(summary.getTriggers().getSimple()).isEmpty();
		assertThat(summary.getTriggers().getDailyTimeInterval()).containsOnlyKeys("every-hour-9am");
		assertThat(summary.getTriggers().getCalendarTimeInterval()).isEmpty();
		assertThat(summary.getTriggers().getCustom()).isEmpty();
	}

	@Test
	void quartzTriggerGroupSummaryWithDailyIntervalTriggerDetails() throws SchedulerException {
		Date previousFireTime = Date.from(Instant.parse("2020-11-30T03:00:00Z"));
		Date nextFireTime = Date.from(Instant.parse("2020-12-01T03:00:00Z"));
		DailyTimeIntervalTrigger trigger = TriggerBuilder.newTrigger().withIdentity("every-hour-tue-thu", "samples")
				.withPriority(4)
				.withSchedule(DailyTimeIntervalScheduleBuilder.dailyTimeIntervalSchedule()
						.onDaysOfTheWeek(Calendar.TUESDAY, Calendar.THURSDAY)
						.startingDailyAt(TimeOfDay.hourAndMinuteOfDay(9, 0))
						.endingDailyAt(TimeOfDay.hourAndMinuteOfDay(18, 0)).withInterval(1, IntervalUnit.HOUR))
				.build();
		((OperableTrigger) trigger).setPreviousFireTime(previousFireTime);
		((OperableTrigger) trigger).setNextFireTime(nextFireTime);
		mockTriggers(trigger);
		QuartzTriggerGroupSummary summary = this.endpoint.quartzTriggerGroupSummary("samples");
		Map<String, Object> triggers = summary.getTriggers().getDailyTimeInterval();
		assertThat(triggers).containsOnlyKeys("every-hour-tue-thu");
		assertThat(triggers).extractingByKey("every-hour-tue-thu", nestedMap()).containsOnly(
				entry("previousFireTime", previousFireTime), entry("nextFireTime", nextFireTime), entry("priority", 4),
				entry("interval", 3600000L), entry("startTimeOfDay", Duration.ofHours(9)),
				entry("endTimeOfDay", Duration.ofHours(18)),
				entry("daysOfWeek", new LinkedHashSet<>(Arrays.asList(3, 5))));
	}

	@Test
	void quartzTriggerGroupSummaryWithCalendarIntervalTrigger() throws SchedulerException {
		CalendarIntervalTrigger trigger = TriggerBuilder.newTrigger().withIdentity("once-a-week", "samples")
				.withSchedule(CalendarIntervalScheduleBuilder.calendarIntervalSchedule().withIntervalInWeeks(1))
				.build();
		mockTriggers(trigger);
		QuartzTriggerGroupSummary summary = this.endpoint.quartzTriggerGroupSummary("samples");
		assertThat(summary.getName()).isEqualTo("samples");
		assertThat(summary.isPaused()).isFalse();
		assertThat(summary.getTriggers().getCron()).isEmpty();
		assertThat(summary.getTriggers().getSimple()).isEmpty();
		assertThat(summary.getTriggers().getDailyTimeInterval()).isEmpty();
		assertThat(summary.getTriggers().getCalendarTimeInterval()).containsOnlyKeys("once-a-week");
		assertThat(summary.getTriggers().getCustom()).isEmpty();
	}

	@Test
	void quartzTriggerGroupSummaryWithCalendarIntervalTriggerDetails() throws SchedulerException {
		TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");
		Date previousFireTime = Date.from(Instant.parse("2020-11-30T03:00:00Z"));
		Date nextFireTime = Date.from(Instant.parse("2020-12-01T03:00:00Z"));
		CalendarIntervalTrigger trigger = TriggerBuilder.newTrigger().withIdentity("once-a-week", "samples")
				.withPriority(8).withSchedule(CalendarIntervalScheduleBuilder.calendarIntervalSchedule()
						.withIntervalInWeeks(1).inTimeZone(timeZone))
				.build();
		((OperableTrigger) trigger).setPreviousFireTime(previousFireTime);
		((OperableTrigger) trigger).setNextFireTime(nextFireTime);
		mockTriggers(trigger);
		QuartzTriggerGroupSummary summary = this.endpoint.quartzTriggerGroupSummary("samples");
		Map<String, Object> triggers = summary.getTriggers().getCalendarTimeInterval();
		assertThat(triggers).containsOnlyKeys("once-a-week");
		assertThat(triggers).extractingByKey("once-a-week", nestedMap()).containsOnly(
				entry("previousFireTime", previousFireTime), entry("nextFireTime", nextFireTime), entry("priority", 8),
				entry("interval", 604800000L), entry("timeZone", timeZone));
	}

	@Test
	void quartzTriggerGroupSummaryWithCustomTrigger() throws SchedulerException {
		Trigger trigger = mock(Trigger.class);
		given(trigger.getKey()).willReturn(TriggerKey.triggerKey("custom", "samples"));
		mockTriggers(trigger);
		QuartzTriggerGroupSummary summary = this.endpoint.quartzTriggerGroupSummary("samples");
		assertThat(summary.getName()).isEqualTo("samples");
		assertThat(summary.isPaused()).isFalse();
		assertThat(summary.getTriggers().getCron()).isEmpty();
		assertThat(summary.getTriggers().getSimple()).isEmpty();
		assertThat(summary.getTriggers().getDailyTimeInterval()).isEmpty();
		assertThat(summary.getTriggers().getCalendarTimeInterval()).isEmpty();
		assertThat(summary.getTriggers().getCustom()).containsOnlyKeys("custom");
	}

	@Test
	void quartzTriggerGroupSummaryWithCustomTriggerDetails() throws SchedulerException {
		Date previousFireTime = Date.from(Instant.parse("2020-11-30T03:00:00Z"));
		Date nextFireTime = Date.from(Instant.parse("2020-12-01T03:00:00Z"));
		Trigger trigger = mock(Trigger.class);
		given(trigger.getKey()).willReturn(TriggerKey.triggerKey("custom", "samples"));
		given(trigger.getPreviousFireTime()).willReturn(previousFireTime);
		given(trigger.getNextFireTime()).willReturn(nextFireTime);
		given(trigger.getPriority()).willReturn(9);
		mockTriggers(trigger);
		QuartzTriggerGroupSummary summary = this.endpoint.quartzTriggerGroupSummary("samples");
		Map<String, Object> triggers = summary.getTriggers().getCustom();
		assertThat(triggers).containsOnlyKeys("custom");
		assertThat(triggers).extractingByKey("custom", nestedMap()).containsOnly(
				entry("previousFireTime", previousFireTime), entry("nextFireTime", nextFireTime), entry("priority", 9),
				entry("trigger", trigger.toString()));
	}

	@Test
	void quartzTriggerWithCronTrigger() throws SchedulerException {
		Date previousFireTime = Date.from(Instant.parse("2020-11-30T03:00:00Z"));
		Date nextFireTime = Date.from(Instant.parse("2020-12-01T03:00:00Z"));
		TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");
		CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity("3am-every-day", "samples").withPriority(3)
				.withDescription("Sample description")
				.withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(3, 0).inTimeZone(timeZone)).build();
		((OperableTrigger) trigger).setPreviousFireTime(previousFireTime);
		((OperableTrigger) trigger).setNextFireTime(nextFireTime);
		mockTriggers(trigger);
		given(this.scheduler.getTriggerState(TriggerKey.triggerKey("3am-every-day", "samples")))
				.willReturn(TriggerState.NORMAL);
		Map<String, Object> triggerDetails = this.endpoint.quartzTrigger("samples", "3am-every-day");
		assertThat(triggerDetails).contains(entry("group", "samples"), entry("name", "3am-every-day"),
				entry("description", "Sample description"), entry("type", "cron"), entry("state", TriggerState.NORMAL),
				entry("priority", 3));
		assertThat(triggerDetails).contains(entry("previousFireTime", previousFireTime),
				entry("nextFireTime", nextFireTime));
		assertThat(triggerDetails).doesNotContainKeys("simple", "dailyTimeInterval", "calendarTimeInterval", "custom");
		assertThat(triggerDetails).extractingByKey("cron", nestedMap()).containsOnly(entry("expression", "0 0 3 ? * *"),
				entry("timeZone", timeZone));
	}

	@Test
	void quartzTriggerWithSimpleTrigger() throws SchedulerException {
		Date startTime = Date.from(Instant.parse("2020-01-01T09:00:00Z"));
		Date previousFireTime = Date.from(Instant.parse("2020-11-30T03:00:00Z"));
		Date nextFireTime = Date.from(Instant.parse("2020-12-01T03:00:00Z"));
		Date endTime = Date.from(Instant.parse("2020-01-31T09:00:00Z"));
		SimpleTrigger trigger = TriggerBuilder.newTrigger().withIdentity("every-hour", "samples").withPriority(20)
				.withDescription("Every hour").startAt(startTime).endAt(endTime)
				.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInHours(1).withRepeatCount(2000))
				.build();
		((OperableTrigger) trigger).setPreviousFireTime(previousFireTime);
		((OperableTrigger) trigger).setNextFireTime(nextFireTime);
		mockTriggers(trigger);
		given(this.scheduler.getTriggerState(TriggerKey.triggerKey("every-hour", "samples")))
				.willReturn(TriggerState.COMPLETE);
		Map<String, Object> triggerDetails = this.endpoint.quartzTrigger("samples", "every-hour");
		assertThat(triggerDetails).contains(entry("group", "samples"), entry("name", "every-hour"),
				entry("description", "Every hour"), entry("type", "simple"), entry("state", TriggerState.COMPLETE),
				entry("priority", 20));
		assertThat(triggerDetails).contains(entry("startTime", startTime), entry("previousFireTime", previousFireTime),
				entry("nextFireTime", nextFireTime), entry("endTime", endTime));
		assertThat(triggerDetails).doesNotContainKeys("cron", "dailyTimeInterval", "calendarTimeInterval", "custom");
		assertThat(triggerDetails).extractingByKey("simple", nestedMap()).containsOnly(entry("interval", 3600000L),
				entry("repeatCount", 2000), entry("timesTriggered", 0));
	}

	@Test
	void quartzTriggerWithDailyTimeIntervalTrigger() throws SchedulerException {
		Date previousFireTime = Date.from(Instant.parse("2020-11-30T03:00:00Z"));
		Date nextFireTime = Date.from(Instant.parse("2020-12-01T03:00:00Z"));
		DailyTimeIntervalTrigger trigger = TriggerBuilder.newTrigger().withIdentity("every-hour-mon-wed", "samples")
				.withDescription("Every working hour Mon Wed").withPriority(4)
				.withSchedule(DailyTimeIntervalScheduleBuilder.dailyTimeIntervalSchedule()
						.onDaysOfTheWeek(Calendar.MONDAY, Calendar.WEDNESDAY)
						.startingDailyAt(TimeOfDay.hourAndMinuteOfDay(9, 0))
						.endingDailyAt(TimeOfDay.hourAndMinuteOfDay(18, 0)).withInterval(1, IntervalUnit.HOUR))
				.build();
		((OperableTrigger) trigger).setPreviousFireTime(previousFireTime);
		((OperableTrigger) trigger).setNextFireTime(nextFireTime);
		mockTriggers(trigger);
		given(this.scheduler.getTriggerState(TriggerKey.triggerKey("every-hour-mon-wed", "samples")))
				.willReturn(TriggerState.NORMAL);
		Map<String, Object> triggerDetails = this.endpoint.quartzTrigger("samples", "every-hour-mon-wed");
		assertThat(triggerDetails).contains(entry("group", "samples"), entry("name", "every-hour-mon-wed"),
				entry("description", "Every working hour Mon Wed"), entry("type", "dailyTimeInterval"),
				entry("state", TriggerState.NORMAL), entry("priority", 4));
		assertThat(triggerDetails).contains(entry("previousFireTime", previousFireTime),
				entry("nextFireTime", nextFireTime));
		assertThat(triggerDetails).doesNotContainKeys("cron", "simple", "calendarTimeInterval", "custom");
		assertThat(triggerDetails).extractingByKey("dailyTimeInterval", nestedMap()).containsOnly(
				entry("interval", 3600000L), entry("startTimeOfDay", Duration.ofHours(9)),
				entry("endTimeOfDay", Duration.ofHours(18)),
				entry("daysOfWeek", new LinkedHashSet<>(Arrays.asList(2, 4))), entry("repeatCount", -1),
				entry("timesTriggered", 0));
	}

	@Test
	void quartzTriggerWithCalendarTimeIntervalTrigger() throws SchedulerException {
		TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");
		Date previousFireTime = Date.from(Instant.parse("2020-11-30T03:00:00Z"));
		Date nextFireTime = Date.from(Instant.parse("2020-12-01T03:00:00Z"));
		CalendarIntervalTrigger trigger = TriggerBuilder.newTrigger().withIdentity("once-a-week", "samples")
				.withDescription("Once a week").withPriority(8)
				.withSchedule(CalendarIntervalScheduleBuilder.calendarIntervalSchedule().withIntervalInWeeks(1)
						.inTimeZone(timeZone).preserveHourOfDayAcrossDaylightSavings(true))
				.build();
		((OperableTrigger) trigger).setPreviousFireTime(previousFireTime);
		((OperableTrigger) trigger).setNextFireTime(nextFireTime);
		mockTriggers(trigger);
		given(this.scheduler.getTriggerState(TriggerKey.triggerKey("once-a-week", "samples")))
				.willReturn(TriggerState.BLOCKED);
		Map<String, Object> triggerDetails = this.endpoint.quartzTrigger("samples", "once-a-week");
		assertThat(triggerDetails).contains(entry("group", "samples"), entry("name", "once-a-week"),
				entry("description", "Once a week"), entry("type", "calendarTimeInterval"),
				entry("state", TriggerState.BLOCKED), entry("priority", 8));
		assertThat(triggerDetails).contains(entry("previousFireTime", previousFireTime),
				entry("nextFireTime", nextFireTime));
		assertThat(triggerDetails).doesNotContainKeys("cron", "simple", "dailyTimeInterval", "custom");
		assertThat(triggerDetails).extractingByKey("calendarTimeInterval", nestedMap()).containsOnly(
				entry("interval", 604800000L), entry("timeZone", timeZone),
				entry("preserveHourOfDayAcrossDaylightSavings", true), entry("skipDayIfHourDoesNotExist", false),
				entry("timesTriggered", 0));
	}

	@Test
	void quartzTriggerWithCustomTrigger() throws SchedulerException {
		Date previousFireTime = Date.from(Instant.parse("2020-11-30T03:00:00Z"));
		Date nextFireTime = Date.from(Instant.parse("2020-12-01T03:00:00Z"));
		Trigger trigger = mock(Trigger.class);
		given(trigger.getKey()).willReturn(TriggerKey.triggerKey("custom", "samples"));
		given(trigger.getPreviousFireTime()).willReturn(previousFireTime);
		given(trigger.getNextFireTime()).willReturn(nextFireTime);
		given(trigger.getPriority()).willReturn(9);
		mockTriggers(trigger);
		given(this.scheduler.getTriggerState(TriggerKey.triggerKey("custom", "samples")))
				.willReturn(TriggerState.ERROR);
		Map<String, Object> triggerDetails = this.endpoint.quartzTrigger("samples", "custom");
		assertThat(triggerDetails).contains(entry("group", "samples"), entry("name", "custom"), entry("type", "custom"),
				entry("state", TriggerState.ERROR), entry("priority", 9));
		assertThat(triggerDetails).contains(entry("previousFireTime", previousFireTime),
				entry("nextFireTime", nextFireTime));
		assertThat(triggerDetails).doesNotContainKeys("cron", "simple", "calendarTimeInterval", "dailyTimeInterval");
		assertThat(triggerDetails).extractingByKey("custom", nestedMap())
				.containsOnly(entry("trigger", trigger.toString()));
	}

	@Test
	void quartzJob() throws SchedulerException {
		JobDetail job = JobBuilder.newJob(Job.class).withIdentity("hello", "samples").withDescription("A sample job")
				.storeDurably().requestRecovery(false).build();
		mockJobs(job);
		QuartzJobDetails jobDetails = this.endpoint.quartzJob("samples", "hello");
		assertThat(jobDetails.getGroup()).isEqualTo("samples");
		assertThat(jobDetails.getName()).isEqualTo("hello");
		assertThat(jobDetails.getDescription()).isEqualTo("A sample job");
		assertThat(jobDetails.getClassName()).isEqualTo(Job.class.getName());
		assertThat(jobDetails.isDurable()).isTrue();
		assertThat(jobDetails.isRequestRecovery()).isFalse();
		assertThat(jobDetails.getData()).isEmpty();
		assertThat(jobDetails.getTriggers()).isEmpty();
	}

	@Test
	void quartzJobWithCronTrigger() throws SchedulerException {
		JobDetail job = JobBuilder.newJob(Job.class).withIdentity("hello", "samples").build();
		TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");
		Trigger cronTrigger = TriggerBuilder.newTrigger().withIdentity("3am-every-day", "samples").withPriority(4)
				.withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(3, 0).inTimeZone(timeZone)).build();
		mockJobs(job);
		mockTriggers(cronTrigger);
		given(this.scheduler.getTriggersOfJob(JobKey.jobKey("hello", "samples")))
				.willAnswer((invocation) -> Collections.singletonList(cronTrigger));
		QuartzJobDetails jobDetails = this.endpoint.quartzJob("samples", "hello");
		assertThat(jobDetails.getTriggers()).hasSize(1);
		Map<String, Object> triggerDetails = jobDetails.getTriggers().get(0);
		assertThat(triggerDetails).containsOnly(entry("group", "samples"), entry("name", "3am-every-day"),
				entry("priority", 4), entry("expression", "0 0 3 ? * *"), entry("timeZone", timeZone));
	}

	private void mockJobs(JobDetail... jobs) throws SchedulerException {
		MultiValueMap<String, JobKey> jobKeys = new LinkedMultiValueMap<>();
		for (JobDetail jobDetail : jobs) {
			JobKey key = jobDetail.getKey();
			given(this.scheduler.getJobDetail(key)).willReturn(jobDetail);
			jobKeys.add(key.getGroup(), key);
		}
		given(this.scheduler.getJobGroupNames()).willReturn(new ArrayList<>(jobKeys.keySet()));
		for (Entry<String, List<JobKey>> entry : jobKeys.entrySet()) {
			given(this.scheduler.getJobKeys(GroupMatcher.jobGroupEquals(entry.getKey())))
					.willReturn(new LinkedHashSet<>(entry.getValue()));
		}
	}

	private void mockTriggers(Trigger... triggers) throws SchedulerException {
		MultiValueMap<String, TriggerKey> triggerKeys = new LinkedMultiValueMap<>();
		for (Trigger trigger : triggers) {
			TriggerKey key = trigger.getKey();
			given(this.scheduler.getTrigger(key)).willReturn(trigger);
			triggerKeys.add(key.getGroup(), key);
		}
		given(this.scheduler.getTriggerGroupNames()).willReturn(new ArrayList<>(triggerKeys.keySet()));
		for (Entry<String, List<TriggerKey>> entry : triggerKeys.entrySet()) {
			given(this.scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(entry.getKey())))
					.willReturn(new LinkedHashSet<>(entry.getValue()));
		}
	}

	@SuppressWarnings("rawtypes")
	private static InstanceOfAssertFactory<Map, MapAssert<String, Object>> nestedMap() {
		return InstanceOfAssertFactories.map(String.class, Object.class);
	}

}
