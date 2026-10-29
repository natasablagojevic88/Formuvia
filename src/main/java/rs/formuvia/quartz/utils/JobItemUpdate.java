package rs.formuvia.quartz.utils;

import java.net.HttpURLConnection;
import java.sql.Connection;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modelmapper.ModelMapper;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import jakarta.ws.rs.WebApplicationException;
import rs.formuvia.database.enums.SearchOperation;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.exceptions.NotNullException;
import rs.formuvia.exceptions.UniqueException;
import rs.formuvia.quartz.dto.JobItemDTO;
import rs.formuvia.quartz.entity.JobCron;
import rs.formuvia.quartz.entity.JobItem;
import rs.formuvia.quartz.enums.JobExportType;
import rs.formuvia.quartz.enums.JobOutputType;
import rs.formuvia.quartz.enums.JobType;
import rs.formuvia.quartz.jobs.ReportJob;
import rs.formuvia.report.entity.Report;
import rs.formuvia.report.enums.ReportType;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

public class JobItemUpdate implements ExecuteQuery<JobItemDTO> {
	private final JobItemDTO jobItemDTO;
	private final Boolean fromReport;
	public final static String REPORT_QUARTZ_GROUP = "REPORT_GROUP";
	public final static String REPORT_ID = "REPORT_ID";

	private final static String MINUTES_TO_REPLACE = "#MINUTES#";
	private final static String HOURS_TO_REPLACE = "#HOURS#";
	private final static String DAYS_TO_REPLACE = "#DAY#";
	private final static String MONTH_TO_REPLACE = "#MONTH#";
	private final static String DAY_OF_WEEK_TO_REPLACE = "#DAY_OF_WEEK#";
	private final static String YEAR_TO_REPLACE = "#YEAR#";
	private final static String STAR_TO_REPLACE = "*";
	private final static String EVERY_STAR_TO_REPLACE = "*/";
	private final static String QUESTION_MARK_TO_REPLACE = "?";

	private final static String CRON_MAKER = "0 #MINUTES# #HOURS# #DAY# #MONTH# #DAY_OF_WEEK# #YEAR#";

	private static Logger logger = LogManager.getLogger(JobItemUpdate.class);

	private DatabaseService databaseService = new DatabaseServiceImpl();
	private ModelMapper modelMapper = new ModelMapper();

	public JobItemUpdate(JobItemDTO jobItemDTO, Boolean fromReport) {
		this.jobItemDTO = jobItemDTO;
		this.fromReport = fromReport;
	}

	@Override
	public JobItemDTO execute(Connection connection) throws Exception {
		checkJobItemDTO(jobItemDTO, fromReport, connection);

		JobItem jobItem = StringUtils.isNull(jobItemDTO.getId()) ? new JobItem()
				: this.databaseService.findById(jobItemDTO.getId(), JobItem.class, connection);

		if (StringUtils.notNull(jobItemDTO.getId()))
			if (!jobItemDTO.getName().equals(jobItem.getName()))
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noNameChangedAllow", null);

		modelMapper.map(jobItemDTO, jobItem);
		JobCron jobCron = this.databaseService.save(jobItem.getJobCron(), connection);
		jobItem.setJobCron(jobCron);
		jobItem = this.databaseService.save(jobItem, connection);
		JobItemDTO jobItemDTOFinal = modelMapper.map(jobItem, JobItemDTO.class);
		loadJob(jobItemDTOFinal);
		return jobItemDTOFinal;
	}

	public static JobDetail createJobDetail(String jobName, UUID reportId) {
		return JobBuilder.newJob(ReportJob.class).storeDurably().usingJobData(REPORT_ID, reportId.toString())
				.withIdentity(jobName, REPORT_QUARTZ_GROUP).build();
	}

	public static void loadJob(JobItemDTO jobItemDTO) {
		if (StaticData.databaseScheduler == null)
			return;

		if (StringUtils.notNull(jobItemDTO.getReportId())) {
			JobDetail jobDetail = createJobDetail(jobItemDTO.getName(), jobItemDTO.getReportId());
			Trigger trigger = TriggerBuilder.newTrigger().withIdentity(jobItemDTO.getName(), REPORT_QUARTZ_GROUP)
					.withSchedule(CronScheduleBuilder.cronSchedule(cronMaker(jobItemDTO))).forJob(jobDetail).build();

			try {
				StaticData.databaseScheduler.addJob(jobDetail, true, true);

				if (jobItemDTO.getActive()) {
					if (StaticData.databaseScheduler.checkExists(trigger.getKey())) {
						Trigger oldTrigger = StaticData.databaseScheduler.getTrigger(trigger.getKey());
						StaticData.databaseScheduler.rescheduleJob(oldTrigger.getKey(), trigger);
					} else {
						StaticData.databaseScheduler.scheduleJob(trigger);
					}

				} else {
					if (StaticData.databaseScheduler.checkExists(trigger.getKey())) {
						Trigger oldTrigger = StaticData.databaseScheduler.getTrigger(trigger.getKey());
						StaticData.databaseScheduler.unscheduleJob(oldTrigger.getKey());
					}
				}
			} catch (SchedulerException e) {
				logger.error(e.getMessage(), e);
				throw new WebApplicationException(e);
			}

		}

	}

	private static String cronMaker(JobItemDTO jobItemDTO) {
		String expresion = new String(CRON_MAKER);
		if (jobItemDTO.getJobCronAnyMinutes()) {
			expresion = expresion.replace(MINUTES_TO_REPLACE, STAR_TO_REPLACE);
		} else {
			expresion = expresion.replace(MINUTES_TO_REPLACE,
					StringUtils.isNull(jobItemDTO.getJobCronEveryMinute()) ? jobItemDTO.getJobCronInMinutes()
							: EVERY_STAR_TO_REPLACE + jobItemDTO.getJobCronEveryMinute().toString());
		}

		if (jobItemDTO.getJobCronAnyHours()) {
			expresion = expresion.replace(HOURS_TO_REPLACE, STAR_TO_REPLACE);
		} else {
			expresion = expresion.replace(HOURS_TO_REPLACE,
					StringUtils.isNull(jobItemDTO.getJobCronEveryHours()) ? jobItemDTO.getJobCronInHours()
							: EVERY_STAR_TO_REPLACE + jobItemDTO.getJobCronEveryHours().toString());
		}

		if (jobItemDTO.getJobCronAnyDay()) {
			expresion = expresion.replace(DAYS_TO_REPLACE,
					jobItemDTO.getJobCronAnyDOfWeek() ? STAR_TO_REPLACE : QUESTION_MARK_TO_REPLACE);
		} else {
			expresion = expresion.replace(DAYS_TO_REPLACE,
					StringUtils.isNull(jobItemDTO.getJobCronEveryDay()) ? jobItemDTO.getJobCronInDay()
							: EVERY_STAR_TO_REPLACE + jobItemDTO.getJobCronEveryDay().toString());
		}

		if (jobItemDTO.getJobCronAnyMonth()) {
			expresion = expresion.replace(MONTH_TO_REPLACE, STAR_TO_REPLACE);
		} else {
			expresion = expresion.replace(MONTH_TO_REPLACE,
					StringUtils.isNull(jobItemDTO.getJobCronEveryMonth()) ? jobItemDTO.getJobCronInMonth()
							: EVERY_STAR_TO_REPLACE + jobItemDTO.getJobCronEveryMonth().toString());
		}

		if (jobItemDTO.getJobCronAnyDOfWeek()) {
			expresion = expresion.replace(DAY_OF_WEEK_TO_REPLACE, QUESTION_MARK_TO_REPLACE);
		} else {
			expresion = expresion.replace(DAY_OF_WEEK_TO_REPLACE, jobItemDTO.getJobCronInDOfWeek());
		}

		if (jobItemDTO.getJobCronAnyYear()) {
			expresion = expresion.replace(YEAR_TO_REPLACE, STAR_TO_REPLACE);
		} else {
			expresion = expresion.replace(YEAR_TO_REPLACE,
					StringUtils.isNull(jobItemDTO.getJobCronEveryYear()) ? jobItemDTO.getJobCronInYear()
							: EVERY_STAR_TO_REPLACE + jobItemDTO.getJobCronEveryYear().toString());
		}

		return expresion;
	}

	private void checkJobItemDTO(JobItemDTO jobItemDTO, Boolean fromReport, Connection connection) {

		if (fromReport) {
			if (StringUtils.isNull(jobItemDTO.getReportId())) {
				throw new NotNullException(UniqueException.findFieldFromList(JobItemDTO.class, "reportId"));
			}
		}

		DatabaseParameter uniqueParameters = DatabaseParameter
				.singleFilter(DatabaseFilter.valueOf("name", jobItemDTO.getName()));

		if (StringUtils.notNull(jobItemDTO.getId()))
			uniqueParameters.getFilters()
					.add(DatabaseFilter.valueOf("id", SearchOperation.NOT_EQUALS, jobItemDTO.getId().toString()));

		if (this.databaseService.exists(uniqueParameters, JobItemDTO.class, connection))
			throw new UniqueException(UniqueException.findFieldFromList(JobItemDTO.class, "name"),
					jobItemDTO.getName());

		if (StringUtils.notNull(jobItemDTO.getReportId())) {
			Report report = this.databaseService.findById(jobItemDTO.getReportId(), Report.class, connection);

			if (report.getType().equals(ReportType.JASPER) || report.getType().equals(ReportType.SQL)) {

				if (!(jobItemDTO.getJobType().equals(JobType.EXPORT_FILE)
						|| jobItemDTO.getJobType().equals(JobType.SEND_MAIL))) {
					throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "wrongType", jobItemDTO.getJobType());
				}

			}

			if (!report.getType().equals(ReportType.SQL))
				jobItemDTO.setConditional(false);

			if (report.getType().equals(ReportType.JASPER)) {
				if (StringUtils.isNull(jobItemDTO.getJobOutputType()))
					throw new NotNullException(UniqueException.findFieldFromList(JobItemDTO.class, "jobOutputType"));

				if (jobItemDTO.getJobOutputType().equals(JobOutputType.INLINE))
					throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "wrongOutputType",
							jobItemDTO.getJobType());
			}

			if (report.getType().equals(ReportType.EXECUTE)) {
				if (!jobItemDTO.getJobType().equals(JobType.EXECUTE)) {
					throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "wrongType", jobItemDTO.getJobType());
				}
			}

		}

		if (jobItemDTO.getJobType().equals(JobType.SEND_MAIL)) {
			if (StringUtils.isNull(jobItemDTO.getJobOutputType()))
				throw new NotNullException(UniqueException.findFieldFromList(JobItemDTO.class, "jobOutputType"));

			if (StringUtils.isNull(jobItemDTO.getReceive()))
				throw new NotNullException(UniqueException.findFieldFromList(JobItemDTO.class, "receive"));

			if (jobItemDTO.getJobOutputType().equals(JobOutputType.ATTACHMENT)) {
				if (StringUtils.isNull(jobItemDTO.getFileName()))
					throw new NotNullException(UniqueException.findFieldFromList(JobItemDTO.class, "fileName"));
			} else {
				jobItemDTO.setFileName(null);
			}

			if (StringUtils.isNull(jobItemDTO.getMailSubject()))
				throw new NotNullException(UniqueException.findFieldFromList(JobItemDTO.class, "mailSubject"));

			if (StringUtils.isNull(jobItemDTO.getSmtpId()))
				throw new NotNullException(UniqueException.findFieldFromList(JobItemDTO.class, "smtpId"));
		} else {
			jobItemDTO.setReceive(null);
			jobItemDTO.setJobOutputType(null);
			jobItemDTO.setMailSubject(null);
			jobItemDTO.setMailBody(null);
		}

		if (StringUtils.notNull(jobItemDTO.getExportType())) {
			if (jobItemDTO.getExportType().equals(JobExportType.CSV)) {
				if (StringUtils.isNull(jobItemDTO.getCsvDelimiter()))
					throw new NotNullException(UniqueException.findFieldFromList(JobItemDTO.class, "csvDelimiter"));
			} else {
				jobItemDTO.setCsvDelimiter(null);
			}

		} else {
			jobItemDTO.setCsvDelimiter(null);
		}

		if (jobItemDTO.getJobType().equals(JobType.EXPORT_FILE)) {
			if (StringUtils.isNull(jobItemDTO.getFileName()))
				throw new NotNullException(UniqueException.findFieldFromList(JobItemDTO.class, "fileName"));

			if (StringUtils.isNull(jobItemDTO.getFilePath()))
				throw new NotNullException(UniqueException.findFieldFromList(JobItemDTO.class, "filePath"));

		} else {
			jobItemDTO.setFilePath(null);
			if (StringUtils.notNull(jobItemDTO.getJobOutputType()))
				if (!jobItemDTO.getJobOutputType().equals(JobOutputType.ATTACHMENT))
					jobItemDTO.setFileName(null);
		}

		if (StringUtils.notNull(jobItemDTO.getId()) && StringUtils.isNull(jobItemDTO.getJobCronId()))
			throw new NotNullException(UniqueException.findFieldFromList(JobItemDTO.class, "jobCronId"));

		if ((!jobItemDTO.getJobCronAnyDay()) && (!jobItemDTO.getJobCronAnyDOfWeek()))
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noDayAllowed", null);

		if (jobItemDTO.getJobCronAnyMinutes()) {
			jobItemDTO.setJobCronEveryMinute(null);
			jobItemDTO.setJobCronInMinutes(null);
		} else {
			if (StringUtils.isNull(jobItemDTO.getJobCronEveryMinute())
					&& StringUtils.isNull(jobItemDTO.getJobCronInMinutes()))
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noMinutesCron", null);

			if (StringUtils.notNull(jobItemDTO.getJobCronEveryMinute())
					&& StringUtils.notNull(jobItemDTO.getJobCronInMinutes()))
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "doubleMinutesCron", null);
		}

		if (jobItemDTO.getJobCronAnyHours()) {
			jobItemDTO.setJobCronEveryHours(null);
			jobItemDTO.setJobCronInHours(null);
		} else {
			if (StringUtils.isNull(jobItemDTO.getJobCronEveryHours())
					&& StringUtils.isNull(jobItemDTO.getJobCronInHours()))
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noHouresCron", null);

			if (StringUtils.notNull(jobItemDTO.getJobCronEveryHours())
					&& StringUtils.notNull(jobItemDTO.getJobCronInHours()))
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "doubleHoursCron", null);
		}

		if (jobItemDTO.getJobCronAnyDay()) {
			jobItemDTO.setJobCronEveryDay(null);
			jobItemDTO.setJobCronInDay(null);
		} else {
			if (StringUtils.isNull(jobItemDTO.getJobCronEveryDay()) && StringUtils.isNull(jobItemDTO.getJobCronInDay()))
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noDayCron", null);

			if (StringUtils.notNull(jobItemDTO.getJobCronEveryDay())
					&& StringUtils.notNull(jobItemDTO.getJobCronInDay()))
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "doubleDayCron", null);
		}

		if (jobItemDTO.getJobCronAnyMonth()) {
			jobItemDTO.setJobCronEveryMonth(null);
			jobItemDTO.setJobCronInMonth(null);
		} else {
			if (StringUtils.isNull(jobItemDTO.getJobCronEveryMonth())
					&& StringUtils.isNull(jobItemDTO.getJobCronInMonth()))
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noMonthCron", null);

			if (StringUtils.notNull(jobItemDTO.getJobCronEveryMonth())
					&& StringUtils.notNull(jobItemDTO.getJobCronInMonth()))
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "doubleMonthCron", null);
		}

		if (jobItemDTO.getJobCronAnyDOfWeek()) {

			jobItemDTO.setJobCronInDOfWeek(null);
		} else {
			if (StringUtils.isNull(jobItemDTO.getJobCronInDOfWeek()))
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noDayOfWeekCron", null);
		}

		if (jobItemDTO.getJobCronAnyYear()) {
			jobItemDTO.setJobCronEveryYear(null);
			jobItemDTO.setJobCronInYear(null);
		} else {
			if (StringUtils.isNull(jobItemDTO.getJobCronEveryYear())
					&& StringUtils.isNull(jobItemDTO.getJobCronInYear()))
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noYearCron", null);

			if (StringUtils.notNull(jobItemDTO.getJobCronEveryYear())
					&& StringUtils.notNull(jobItemDTO.getJobCronInYear()))
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "doubleYearCron", null);
		}

	}

}
