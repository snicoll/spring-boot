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
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.quartz.CalendarIntervalTrigger;
import org.quartz.CronTrigger;
import org.quartz.DailyTimeIntervalTrigger;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.TimeOfDay;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.util.Assert;

/**
 * {@link Endpoint} to expose Quartz Scheduler jobs and triggers.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @since 2.5.0
 */
@Endpoint(id = "quartz")
public class QuartzEndpoint {

	private static final Comparator<Trigger> TRIGGER_COMPARATOR = Comparator
			.comparing(Trigger::getNextFireTime, Comparator.nullsLast(Comparator.naturalOrder()))
			.thenComparingInt(Trigger::getPriority);

	private final Scheduler scheduler;

	public QuartzEndpoint(Scheduler scheduler) {
		Assert.notNull(scheduler, "Scheduler must not be null");
		this.scheduler = scheduler;
	}

	/**
	 * Return the available job and trigger group names.
	 * @return a report of the available group names
	 * @throws SchedulerException if retrieving the information from the scheduler failed
	 */
	@ReadOperation
	public QuartzReport quartzReport() throws SchedulerException {
		return new QuartzReport(new GroupNames(this.scheduler.getJobGroupNames()),
				new GroupNames(this.scheduler.getTriggerGroupNames()));
	}

	/**
	 * Return the available job names, identified by group name.
	 * @return the available job names
	 * @throws SchedulerException if retrieving the information from the scheduler failed
	 */
	public Map<String, Object> quartzJobGroups() throws SchedulerException {
		Map<String, Object> result = new LinkedHashMap<>();
		for (String groupName : this.scheduler.getJobGroupNames()) {
			List<String> jobs = this.scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName)).stream()
					.map((key) -> key.getName()).collect(Collectors.toList());
			result.put(groupName, Collections.singletonMap("jobs", jobs));
		}
		return result;
	}

	/**
	 * Return the available trigger names, identified by group name.
	 * @return the available trigger names
	 * @throws SchedulerException if retrieving the information from the scheduler failed
	 */
	public Map<String, Object> quartzTriggerGroups() throws SchedulerException {
		Map<String, Object> result = new LinkedHashMap<>();
		Set<String> pausedTriggerGroups = this.scheduler.getPausedTriggerGroups();
		for (String groupName : this.scheduler.getTriggerGroupNames()) {
			Map<String, Object> groupDetails = new LinkedHashMap<>();
			groupDetails.put("paused", pausedTriggerGroups.contains(groupName));
			groupDetails.put("triggers", this.scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(groupName))
					.stream().map((key) -> key.getName()).collect(Collectors.toList()));
			result.put(groupName, groupDetails);
		}
		return result;
	}

	public QuartzJobGroupSummary quartzJobGroupSummary(String group) throws SchedulerException {
		List<JobDetail> jobs = findJobsByGroup(group);
		if (jobs.isEmpty() && !this.scheduler.getJobGroupNames().contains(group)) {
			return null;
		}
		Map<String, QuartzJobSummary> result = new LinkedHashMap<>();
		for (JobDetail job : jobs) {
			result.put(job.getKey().getName(), QuartzJobSummary.of(job));
		}
		return new QuartzJobGroupSummary(group, result);
	}

	private List<JobDetail> findJobsByGroup(String group) throws SchedulerException {
		List<JobDetail> jobs = new ArrayList<>();
		Set<JobKey> jobKeys = this.scheduler.getJobKeys(GroupMatcher.jobGroupEquals(group));
		for (JobKey jobKey : jobKeys) {
			jobs.add(this.scheduler.getJobDetail(jobKey));
		}
		return jobs;
	}

	public QuartzTriggerGroupSummary quartzTriggerGroupSummary(String group) throws SchedulerException {
		List<Trigger> triggers = findTriggersByGroup(group);
		if (triggers.isEmpty() && !this.scheduler.getTriggerGroupNames().contains(group)) {
			return null;
		}
		triggers.sort(TRIGGER_COMPARATOR);
		Map<TriggerType, Map<String, Object>> result = new LinkedHashMap<>();
		triggers.forEach((trigger) -> {
			TriggerDescription triggerDescription = TriggerDescription.of(trigger);
			Map<String, Object> triggerTypes = result.computeIfAbsent(triggerDescription.getType(),
					(key) -> new LinkedHashMap<>());
			triggerTypes.put(trigger.getKey().getName(), triggerDescription.buildSummary());
		});
		boolean paused = this.scheduler.getPausedTriggerGroups().contains(group);
		return new QuartzTriggerGroupSummary(group, paused, result);
	}

	private List<Trigger> findTriggersByGroup(String group) throws SchedulerException {
		List<Trigger> triggers = new ArrayList<>();
		Set<TriggerKey> triggerKeys = this.scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(group));
		for (TriggerKey triggerKey : triggerKeys) {
			triggers.add(this.scheduler.getTrigger(triggerKey));
		}
		return triggers;
	}

	/**
	 * Return the {@link QuartzJobDetails details of the job} identified with the given
	 * group name and job name.
	 * @param groupName the name of the group
	 * @param jobName the name of the job
	 * @return the details of the job
	 * @throws SchedulerException if retrieving the information from the scheduler failed
	 */
	public QuartzJobDetails quartzJob(String groupName, String jobName) throws SchedulerException {
		JobKey jobKey = JobKey.jobKey(jobName, groupName);
		JobDetail jobDetail = this.scheduler.getJobDetail(jobKey);
		List<? extends Trigger> triggers = this.scheduler.getTriggersOfJob(jobKey);
		return new QuartzJobDetails(jobDetail, triggers);
	}

	/**
	 * Return the details of the trigger identified by the given group name and trigger
	 * name.
	 * @param triggerGroup the name of the group
	 * @param triggerName the name of the trigger
	 * @return the details of the trigger
	 * @throws SchedulerException if retrieving the information from the scheduler failed
	 */
	public Map<String, Object> quartzTrigger(String triggerGroup, String triggerName) throws SchedulerException {
		TriggerKey triggerKey = TriggerKey.triggerKey(triggerName, triggerGroup);
		Trigger trigger = this.scheduler.getTrigger(triggerKey);
		return TriggerDescription.of(trigger).buildDetails(this.scheduler.getTriggerState(triggerKey));
	}

	private static Duration getIntervalDuration(long amount, IntervalUnit unit) {
		return temporalUnit(unit).getDuration().multipliedBy(amount);
	}

	private static Duration getTimeOfDayDuration(TimeOfDay timeOfDay) {
		return (timeOfDay != null) ? Duration.ofHours(timeOfDay.getHour())
				.plus(Duration.ofMinutes(timeOfDay.getMinute())).plus(Duration.ofSeconds(timeOfDay.getSecond())) : null;
	}

	private static TemporalUnit temporalUnit(IntervalUnit unit) {
		switch (unit) {
		case DAY:
			return ChronoUnit.DAYS;
		case HOUR:
			return ChronoUnit.HOURS;
		case MINUTE:
			return ChronoUnit.MINUTES;
		case MONTH:
			return ChronoUnit.MONTHS;
		case SECOND:
			return ChronoUnit.SECONDS;
		case MILLISECOND:
			return ChronoUnit.MILLIS;
		case WEEK:
			return ChronoUnit.WEEKS;
		case YEAR:
			return ChronoUnit.YEARS;
		default:
			throw new IllegalArgumentException("Unknown IntervalUnit");
		}
	}

	public static final class QuartzReport {

		private final GroupNames jobs;

		private final GroupNames triggers;

		QuartzReport(GroupNames jobs, GroupNames triggers) {
			this.jobs = jobs;
			this.triggers = triggers;
		}

		public GroupNames getJobs() {
			return this.jobs;
		}

		public GroupNames getTriggers() {
			return this.triggers;
		}

	}

	public static class GroupNames {

		private final Set<String> groups;

		public GroupNames(List<String> groups) {
			this.groups = new LinkedHashSet<>(groups);
		}

		public Set<String> getGroups() {
			return this.groups;
		}

	}

	/**
	 * * A summary report of the {@link JobDetail jobs} in a given group.
	 */
	public static final class QuartzJobGroupSummary {

		private final String name;

		private final Map<String, QuartzJobSummary> jobs;

		private QuartzJobGroupSummary(String name, Map<String, QuartzJobSummary> jobs) {
			this.name = name;
			this.jobs = jobs;
		}

		public String getName() {
			return this.name;
		}

		public Map<String, QuartzJobSummary> getJobs() {
			return this.jobs;
		}

	}

	/**
	 * Details of a {@link Job Quartz Job}, primarily intended for serialization to JSON.
	 */
	public static final class QuartzJobSummary {

		private final String className;

		private QuartzJobSummary(JobDetail job) {
			this.className = job.getJobClass().getName();
		}

		private static QuartzJobSummary of(JobDetail job) {
			return new QuartzJobSummary(job);
		}

		public String getClassName() {
			return this.className;
		}

	}

	/**
	 * Details of a {@link Job Quartz Job}, primarily intended for serialization to JSON.
	 */
	public static final class QuartzJobDetails {

		private final String group;

		private final String name;

		private final String description;

		private final String className;

		private final boolean durable;

		private final boolean requestRecovery;

		private final Map<String, Object> data;

		private final List<Map<String, Object>> triggers;

		QuartzJobDetails(JobDetail jobDetail, List<? extends Trigger> triggers) {
			this.group = jobDetail.getKey().getGroup();
			this.name = jobDetail.getKey().getName();
			this.description = jobDetail.getDescription();
			this.className = jobDetail.getJobClass().getName();
			this.durable = jobDetail.isDurable();
			this.requestRecovery = jobDetail.requestsRecovery();
			this.data = jobDetail.getJobDataMap().getWrappedMap();
			this.triggers = extractTriggersSummary(triggers);
		}

		private static List<Map<String, Object>> extractTriggersSummary(List<? extends Trigger> triggers) {
			List<Trigger> triggersToSort = new ArrayList<>(triggers);
			triggersToSort.sort(TRIGGER_COMPARATOR);
			List<Map<String, Object>> result = new ArrayList<>();
			triggersToSort.forEach((trigger) -> {
				Map<String, Object> triggerSummary = new LinkedHashMap<>();
				triggerSummary.put("group", trigger.getKey().getGroup());
				triggerSummary.put("name", trigger.getKey().getName());
				triggerSummary.putAll(TriggerDescription.of(trigger).buildSummary());
				result.add(triggerSummary);
			});
			return result;
		}

		public String getGroup() {
			return this.group;
		}

		public String getName() {
			return this.name;
		}

		public String getDescription() {
			return this.description;
		}

		public String getClassName() {
			return this.className;
		}

		public boolean isDurable() {
			return this.durable;
		}

		public boolean isRequestRecovery() {
			return this.requestRecovery;
		}

		public Map<String, Object> getData() {
			return this.data;
		}

		public List<Map<String, Object>> getTriggers() {
			return this.triggers;
		}

	}

	/**
	 * A summary report of the {@link Trigger triggers} in a given group.
	 */
	public static final class QuartzTriggerGroupSummary {

		private final String name;

		private final boolean paused;

		private final Triggers triggers;

		private QuartzTriggerGroupSummary(String name, boolean paused,
				Map<TriggerType, Map<String, Object>> descriptionsByType) {
			this.name = name;
			this.paused = paused;
			this.triggers = new Triggers(descriptionsByType);

		}

		public String getName() {
			return this.name;
		}

		public boolean isPaused() {
			return this.paused;
		}

		public Triggers getTriggers() {
			return this.triggers;
		}

		public static final class Triggers {

			private final Map<String, Object> cron;

			private final Map<String, Object> simple;

			private final Map<String, Object> dailyTimeInterval;

			private final Map<String, Object> calendarTimeInterval;

			private final Map<String, Object> custom;

			private Triggers(Map<TriggerType, Map<String, Object>> descriptionsByType) {
				this.cron = descriptionsByType.getOrDefault(TriggerType.CRON, Collections.emptyMap());
				this.dailyTimeInterval = descriptionsByType.getOrDefault(TriggerType.DAILY_INTERVAL,
						Collections.emptyMap());
				this.calendarTimeInterval = descriptionsByType.getOrDefault(TriggerType.CALENDAR_INTERVAL,
						Collections.emptyMap());
				this.simple = descriptionsByType.getOrDefault(TriggerType.SIMPLE, Collections.emptyMap());
				this.custom = descriptionsByType.getOrDefault(TriggerType.CUSTOM_TRIGGER, Collections.emptyMap());
			}

			public Map<String, Object> getCron() {
				return this.cron;
			}

			public Map<String, Object> getSimple() {
				return this.simple;
			}

			public Map<String, Object> getDailyTimeInterval() {
				return this.dailyTimeInterval;
			}

			public Map<String, Object> getCalendarTimeInterval() {
				return this.calendarTimeInterval;
			}

			public Map<String, Object> getCustom() {
				return this.custom;
			}

		}

	}

	private enum TriggerType {

		CRON("cron"),

		CUSTOM_TRIGGER("custom"),

		CALENDAR_INTERVAL("calendarTimeInterval"),

		DAILY_INTERVAL("dailyTimeInterval"),

		SIMPLE("simple");

		private final String id;

		TriggerType(String id) {
			this.id = id;
		}

		public String getId() {
			return this.id;
		}

	}

	/**
	 * Base class for descriptions of a {@link Trigger}.
	 */
	public abstract static class TriggerDescription {

		private static final Map<Class<? extends Trigger>, Function<Trigger, TriggerDescription>> DESCRIBERS = new LinkedHashMap<>();

		static {
			DESCRIBERS.put(CronTrigger.class, (trigger) -> new CronTriggerDescription((CronTrigger) trigger));
			DESCRIBERS.put(SimpleTrigger.class, (trigger) -> new SimpleTriggerDescription((SimpleTrigger) trigger));
			DESCRIBERS.put(DailyTimeIntervalTrigger.class,
					(trigger) -> new DailyTimeIntervalTriggerDescription((DailyTimeIntervalTrigger) trigger));
			DESCRIBERS.put(CalendarIntervalTrigger.class,
					(trigger) -> new CalendarIntervalTriggerDescription((CalendarIntervalTrigger) trigger));
		}

		private final Trigger trigger;

		private final TriggerType type;

		private static TriggerDescription of(Trigger trigger) {
			return DESCRIBERS.entrySet().stream().filter((entry) -> entry.getKey().isInstance(trigger))
					.map((entry) -> entry.getValue().apply(trigger)).findFirst()
					.orElse(new CustomTriggerDescription(trigger));
		}

		protected TriggerDescription(Trigger trigger, TriggerType type) {
			this.trigger = trigger;
			this.type = type;
		}

		public Map<String, Object> buildSummary() {
			Map<String, Object> summary = new LinkedHashMap<>();
			putIfNoNull(summary, "previousFireTime", this.trigger.getPreviousFireTime());
			putIfNoNull(summary, "nextFireTime", this.trigger.getNextFireTime());
			putIfNoNull(summary, "priority", this.trigger.getPriority());
			appendSummary(summary);
			return summary;
		}

		/**
		 * Append trigger-implementation specific summary items to the specified
		 * {@code content}.
		 * @param content the summary of the trigger
		 */
		protected abstract void appendSummary(Map<String, Object> content);

		public Map<String, Object> buildDetails(TriggerState triggerState) {
			Map<String, Object> details = new LinkedHashMap<>();
			details.put("group", this.trigger.getKey().getGroup());
			details.put("name", this.trigger.getKey().getName());
			putIfNoNull(details, "description", this.trigger.getDescription());
			details.put("state", triggerState);
			details.put("type", getType().getId());
			putIfNoNull(details, "calendarName", this.trigger.getCalendarName());
			putIfNoNull(details, "startTime", this.trigger.getStartTime());
			putIfNoNull(details, "endTime", this.trigger.getEndTime());
			putIfNoNull(details, "previousFireTime", this.trigger.getPreviousFireTime());
			putIfNoNull(details, "nextFireTime", this.trigger.getNextFireTime());
			putIfNoNull(details, "priority", this.trigger.getPriority());
			putIfNoNull(details, "finalFireTime", this.trigger.getFinalFireTime());
			Map<String, Object> typeDetails = new LinkedHashMap<>();
			appendDetails(typeDetails);
			details.put(getType().getId(), typeDetails);
			return details;
		}

		/**
		 * Append trigger-implementation specific details to the specified
		 * {@code content}.
		 * @param content the details of the trigger
		 */
		protected abstract void appendDetails(Map<String, Object> content);

		protected void putIfNoNull(Map<String, Object> content, String key, Object value) {
			if (value != null) {
				content.put(key, value);
			}
		}

		protected Trigger getTrigger() {
			return this.trigger;
		}

		protected TriggerType getType() {
			return this.type;
		}

	}

	/**
	 * A description of a {@link CronTrigger}.
	 */
	public static final class CronTriggerDescription extends TriggerDescription {

		private final CronTrigger trigger;

		public CronTriggerDescription(CronTrigger trigger) {
			super(trigger, TriggerType.CRON);
			this.trigger = trigger;
		}

		@Override
		protected void appendSummary(Map<String, Object> content) {
			content.put("expression", this.trigger.getCronExpression());
			if (this.trigger.getTimeZone() != null) {
				content.put("timeZone", this.trigger.getTimeZone());
			}
		}

		@Override
		protected void appendDetails(Map<String, Object> content) {
			appendSummary(content);
		}

	}

	/**
	 * A description of a {@link SimpleTrigger}.
	 */
	public static final class SimpleTriggerDescription extends TriggerDescription {

		private final SimpleTrigger trigger;

		public SimpleTriggerDescription(SimpleTrigger trigger) {
			super(trigger, TriggerType.SIMPLE);
			this.trigger = trigger;
		}

		@Override
		protected void appendSummary(Map<String, Object> content) {
			content.put("interval", this.trigger.getRepeatInterval());
		}

		@Override
		protected void appendDetails(Map<String, Object> content) {
			appendSummary(content);
			content.put("repeatCount", this.trigger.getRepeatCount());
			content.put("timesTriggered", this.trigger.getTimesTriggered());
		}

	}

	/**
	 * A description of a {@link DailyTimeIntervalTrigger}.
	 */
	public static final class DailyTimeIntervalTriggerDescription extends TriggerDescription {

		private final DailyTimeIntervalTrigger trigger;

		public DailyTimeIntervalTriggerDescription(DailyTimeIntervalTrigger trigger) {
			super(trigger, TriggerType.DAILY_INTERVAL);
			this.trigger = trigger;
		}

		@Override
		protected void appendSummary(Map<String, Object> content) {
			content.put("interval",
					getIntervalDuration(this.trigger.getRepeatInterval(), this.trigger.getRepeatIntervalUnit())
							.toMillis());
			putIfNoNull(content, "daysOfWeek", this.trigger.getDaysOfWeek());
			putIfNoNull(content, "startTimeOfDay", getTimeOfDayDuration(this.trigger.getStartTimeOfDay()));
			putIfNoNull(content, "endTimeOfDay", getTimeOfDayDuration(this.trigger.getEndTimeOfDay()));
		}

		@Override
		protected void appendDetails(Map<String, Object> content) {
			appendSummary(content);
			content.put("timesTriggered", this.trigger.getTimesTriggered());
			content.put("repeatCount", this.trigger.getRepeatCount());
		}

	}

	/**
	 * A description of a {@link CalendarIntervalTrigger}.
	 */
	public static final class CalendarIntervalTriggerDescription extends TriggerDescription {

		private final CalendarIntervalTrigger trigger;

		public CalendarIntervalTriggerDescription(CalendarIntervalTrigger trigger) {
			super(trigger, TriggerType.CALENDAR_INTERVAL);
			this.trigger = trigger;
		}

		@Override
		protected void appendSummary(Map<String, Object> content) {
			content.put("interval",
					getIntervalDuration(this.trigger.getRepeatInterval(), this.trigger.getRepeatIntervalUnit())
							.toMillis());
			putIfNoNull(content, "timeZone", this.trigger.getTimeZone());
		}

		@Override
		protected void appendDetails(Map<String, Object> content) {
			appendSummary(content);
			content.put("timesTriggered", this.trigger.getTimesTriggered());
			content.put("preserveHourOfDayAcrossDaylightSavings",
					this.trigger.isPreserveHourOfDayAcrossDaylightSavings());
			content.put("skipDayIfHourDoesNotExist", this.trigger.isSkipDayIfHourDoesNotExist());
		}

	}

	/**
	 * A description of a custom {@link Trigger}.
	 */
	public static final class CustomTriggerDescription extends TriggerDescription {

		public CustomTriggerDescription(Trigger trigger) {
			super(trigger, TriggerType.CUSTOM_TRIGGER);
		}

		@Override
		protected void appendSummary(Map<String, Object> content) {
			content.put("trigger", getTrigger().toString());
		}

		@Override
		protected void appendDetails(Map<String, Object> content) {
			appendSummary(content);
		}

	}

}
