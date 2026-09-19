package rs.formuvia.quartz.service.impl;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.Service;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import jakarta.ws.rs.WebApplicationException;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.impl.AppStartUpImpl;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.quartz.jobs.ClearTokenJob;
import rs.formuvia.quartz.jobs.DatabaseListenCheckConnectionJob;
import rs.formuvia.quartz.jobs.DatabaseListenJob;
import rs.formuvia.quartz.service.QuartzService;
import rs.formuvia.utils.StaticData;

@Service
public class QuartzServiceImpl implements QuartzService {
	private Logger logger = LogManager.getLogger(getClass());
	private final String DATABASE_LISTEN_CRON = "database.listen.cron";
	private final String DATABASE_LISTEN_GROUP = "database.listen";
	private final String DATABASE_LISTEN_NAME = "database.listen";
	private final String DATABASE_LISTEN_CHECK_CONNECTION_CRON = "database.listen.connection.check.cron";
	private final String DATABASE_LISTEN_CHECK_CONNECTION_NAME = "database.listen.check.connection";
	private final String QUARTZ_DATABASE_START = "quartz.database.start";
	private DatabaseService databaseService = new DatabaseServiceImpl();
	private CommonService commonService = new CommonServiceImpl();
	private final String QUARTZ_QUERY = "quartz_schema.sql";
	private final String QUARTZ_DATABASE_URL = "org.quartz.dataSource.formuvia.URL";
	private final String QUARTZ_DATABASE_USER = "org.quartz.dataSource.formuvia.user";
	private final String QUARTZ_DATABASE_PASSWORD = "org.quartz.dataSource.formuvia.password";
	private final String QUARTZ_PROPERTIES_FILE = "quartz-database.properties";
	private final String CLEAR_TOKEN_CRON = "token.clear.cron";
	private final String CLEAR_TOKEN_NAME = "token.clear";

	@Override
	public void initScheduler() {
		try {
			StaticData.localScheduler = new StdSchedulerFactory().getScheduler();
			StaticData.localScheduler.start();
			DatabaseListenCheckConnectionJob.checkConnection();
			initDatabaseListen();
			initDatabaseListenCheckConnection();

			if (!Boolean.valueOf(StaticData.appProperties.getProperty(QUARTZ_DATABASE_START))) {
				return;
			}

			String quartzSchema = this.commonService.readQueryFromFile(QUARTZ_QUERY);
			this.databaseService.executeUpdateQuery(quartzSchema, null);
			StaticData.databaseScheduler = new StdSchedulerFactory(propertiesForDatabase()).getScheduler();
			StaticData.databaseScheduler.start();
			loadCheckTokenJob();

		} catch (Exception e) {
			this.logger.error(e.getMessage(), e);
		}

	}

	private Properties propertiesForDatabase() throws Exception {
		Properties properties = new Properties();
		properties.load(this.getClass().getClassLoader().getResourceAsStream(QUARTZ_PROPERTIES_FILE));
		properties.put(QUARTZ_DATABASE_URL, StaticData.appProperties.get(AppStartUpImpl.CONNECTION_URL));
		properties.put(QUARTZ_DATABASE_USER, StaticData.appProperties.get(AppStartUpImpl.CONNECTION_USERNAME));
		properties.put(QUARTZ_DATABASE_PASSWORD, StaticData.appProperties.get(AppStartUpImpl.CONNECTION_PASSWORD));
		return properties;
	}

	private void initDatabaseListen() {
		JobDetail jobDetail = JobBuilder.newJob(DatabaseListenJob.class).storeDurably()
				.withIdentity(DATABASE_LISTEN_NAME, DATABASE_LISTEN_GROUP).build();
		Trigger trigger = TriggerBuilder.newTrigger().forJob(jobDetail)
				.withIdentity(DATABASE_LISTEN_NAME, DATABASE_LISTEN_GROUP)
				.withSchedule(
						CronScheduleBuilder.cronSchedule(StaticData.appProperties.getProperty(DATABASE_LISTEN_CRON)))
				.build();
		try {
			StaticData.localScheduler.scheduleJob(jobDetail, trigger);
		} catch (SchedulerException e) {
			this.logger.error(e.getMessage(), e);
		}
	}

	private void initDatabaseListenCheckConnection() {
		JobDetail jobDetail = JobBuilder.newJob(DatabaseListenCheckConnectionJob.class).storeDurably()
				.withIdentity(DATABASE_LISTEN_CHECK_CONNECTION_NAME, DATABASE_LISTEN_GROUP).build();
		Trigger trigger = TriggerBuilder.newTrigger().forJob(jobDetail)
				.withIdentity(DATABASE_LISTEN_CHECK_CONNECTION_NAME, DATABASE_LISTEN_GROUP)
				.withSchedule(CronScheduleBuilder
						.cronSchedule(StaticData.appProperties.getProperty(DATABASE_LISTEN_CHECK_CONNECTION_CRON)))
				.build();
		try {
			StaticData.localScheduler.scheduleJob(jobDetail, trigger);
		} catch (SchedulerException e) {
			this.logger.error(e.getMessage(), e);
		}
	}

	private void loadCheckTokenJob() {
		JobDetail jobDetail = JobBuilder.newJob(ClearTokenJob.class).storeDurably()
				.withIdentity(CLEAR_TOKEN_NAME, CLEAR_TOKEN_NAME).build();
		Trigger trigger = TriggerBuilder.newTrigger().forJob(jobDetail).withIdentity(CLEAR_TOKEN_NAME, CLEAR_TOKEN_NAME)
				.withSchedule(CronScheduleBuilder.cronSchedule(StaticData.appProperties.getProperty(CLEAR_TOKEN_CRON)))
				.build();
		try {
			StaticData.databaseScheduler.scheduleJob(jobDetail, trigger);
		} catch (SchedulerException e) {
			this.logger.error(e.getMessage(), e);
			throw new WebApplicationException(e);
		}
	}

	@Override
	public void closeScheduler() {
		try {
			StaticData.localScheduler.shutdown(true);
		} catch (Exception e) {
			this.logger.error(e.getMessage(), e);
		}

		if (StaticData.databaseScheduler != null) {
			try {
				StaticData.databaseScheduler.shutdown(true);
			} catch (SchedulerException e) {
				this.logger.error(e.getMessage(), e);
			}
		}
	}

}
