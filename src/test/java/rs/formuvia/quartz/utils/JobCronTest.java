package rs.formuvia.quartz.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.quartz.CronExpression;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Trigger;

import rs.formuvia.quartz.dto.JobItemDTO;

/**
 * The job dialog keeps the timetable as "any / every n-th / chosen values" per unit of time, and
 * JobItemUpdate builds the Quartz expression out of it. The front shows the same expression
 * (FormuviaFront/src/app/shared/cron-value.ts, cronExpression), so both must build it the same way.
 * Quartz refuses an expression it cannot read, and then the job cannot be saved at all.
 */
class JobCronTest {

	/** Every unit "any", as a new job starts in the dialog. */
	private static JobItemDTO anyTime() {
		JobItemDTO job = new JobItemDTO();
		job.setName("test");
		job.setActive(true);
		job.setJobCronAnyMinutes(true);
		job.setJobCronAnyHours(true);
		job.setJobCronAnyDay(true);
		job.setJobCronAnyMonth(true);
		job.setJobCronAnyDOfWeek(true);
		job.setJobCronAnyYear(true);
		return job;
	}

	private static CronTrigger trigger(JobItemDTO job) {
		JobDetail detail = JobItemUpdate.createJobDetail(job.getName(), UUID.randomUUID(), UUID.randomUUID(), null);
		Trigger trigger = JobItemUpdate.createTrigger(job, detail);
		return (CronTrigger) trigger;
	}

	private static String cron(JobItemDTO job) {
		return trigger(job).getCronExpression();
	}

	/**
	 * Next run after the given moment, from the expression alone. Not trigger.getFireTimeAfter: the
	 * trigger starts now and counts from whichever is later, so a fixed date in the test would start
	 * failing once the real clock passes it.
	 */
	private static LocalDateTime nextAfter(JobItemDTO job, LocalDateTime time) {
		CronExpression expression = assertDoesNotThrow(() -> new CronExpression(cron(job)));
		Date after = Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
		return LocalDateTime.ofInstant(expression.getNextValidTimeAfter(after).toInstant(), ZoneId.systemDefault());
	}

	@Test
	void anyTimeRunsEveryMinute() {
		assertEquals("0 * * * * ? *", cron(anyTime()));
	}

	@Test
	void anyMinuteBesideChosenHoursMeansTheTopOfTheHour() {
		JobItemDTO job = anyTime();
		job.setJobCronAnyHours(false);
		job.setJobCronInHours("8,14");

		assertEquals("0 0 8,14 * * ? *", cron(job));
		assertEquals(LocalDateTime.of(2026, 11, 18, 14, 0), nextAfter(job, LocalDateTime.of(2026, 11, 18, 9, 30)));
	}

	@Test
	void stepsUseStar() {
		JobItemDTO job = anyTime();
		job.setJobCronAnyMinutes(false);
		job.setJobCronEveryMinute(15);
		job.setJobCronAnyHours(false);
		job.setJobCronEveryHours(2);

		assertEquals("0 */15 */2 * * ? *", cron(job));
	}

	@Test
	void chosenWeekdaysPutAQuestionMarkInTheDayOfMonth() {
		JobItemDTO job = anyTime();
		job.setJobCronAnyMinutes(false);
		job.setJobCronInMinutes("30");
		job.setJobCronAnyHours(false);
		job.setJobCronInHours("7");
		job.setJobCronAnyDOfWeek(false);
		job.setJobCronInDOfWeek("2,6");

		assertEquals("0 30 7 ? * 2,6 *", cron(job));
		// 2026-11-18 is a Wednesday; weekday 6 in Quartz is Friday
		assertEquals(LocalDateTime.of(2026, 11, 20, 7, 30), nextAfter(job, LocalDateTime.of(2026, 11, 18, 12, 0)));
	}

	@Test
	void chosenDaysOfTheMonthAndMonths() {
		JobItemDTO job = anyTime();
		job.setJobCronAnyMinutes(false);
		job.setJobCronInMinutes("0");
		job.setJobCronAnyHours(false);
		job.setJobCronInHours("6");
		job.setJobCronAnyDay(false);
		job.setJobCronInDay("1,15");
		job.setJobCronAnyMonth(false);
		job.setJobCronInMonth("1,7");

		assertEquals("0 0 6 1,15 1,7 ? *", cron(job));
		assertEquals(LocalDateTime.of(2027, 1, 1, 6, 0), nextAfter(job, LocalDateTime.of(2026, 11, 18, 12, 0)));
	}

	@Test
	void everyNthDayMonthAndYearAreAcceptedByQuartz() {
		JobItemDTO job = anyTime();
		job.setJobCronAnyMinutes(false);
		job.setJobCronInMinutes("0");
		job.setJobCronAnyHours(false);
		job.setJobCronInHours("0");
		job.setJobCronAnyDay(false);
		job.setJobCronEveryDay(2);
		job.setJobCronAnyMonth(false);
		job.setJobCronEveryMonth(3);
		job.setJobCronAnyYear(false);
		job.setJobCronEveryYear(2);

		assertEquals("0 0 0 */2 */3 ? */2", assertDoesNotThrow(() -> cron(job)));
	}

	@Test
	void chosenYears() {
		JobItemDTO job = anyTime();
		job.setJobCronAnyMinutes(false);
		job.setJobCronInMinutes("0");
		job.setJobCronAnyHours(false);
		job.setJobCronInHours("0");
		job.setJobCronAnyDay(false);
		job.setJobCronInDay("1");
		job.setJobCronAnyMonth(false);
		job.setJobCronInMonth("1");
		job.setJobCronAnyYear(false);
		job.setJobCronInYear("2027,2030");

		assertEquals(LocalDateTime.of(2027, 1, 1, 0, 0), nextAfter(job, LocalDateTime.of(2026, 11, 18, 12, 0)));
	}

	@Test
	void triggerAndJobHaveTheNameOfTheJobInTheReportGroup() {
		JobItemDTO job = anyTime();
		job.setName("Monthly invoices");
		CronTrigger trigger = trigger(job);

		assertEquals("Monthly invoices", trigger.getKey().getName());
		assertEquals(JobItemUpdate.REPORT_QUARTZ_GROUP, trigger.getKey().getGroup());
		assertEquals(trigger.getJobKey().getName(), trigger.getKey().getName());
	}

	@Test
	void jobDetailCarriesTheIdentifiers() {
		UUID jobId = UUID.randomUUID();
		UUID modelId = UUID.randomUUID();

		JobDetail detail = JobItemUpdate.createJobDetail("import", null, jobId, modelId);

		assertEquals(jobId.toString(), detail.getJobDataMap().getString(JobItemUpdate.JOB_ID));
		assertEquals(modelId.toString(), detail.getJobDataMap().getString(JobItemUpdate.MODEL_ID));
		assertEquals(null, detail.getJobDataMap().getString(JobItemUpdate.REPORT_ID));
	}
}
