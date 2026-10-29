package rs.formuvia.quartz.utils;

import java.sql.Connection;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;

import jakarta.ws.rs.WebApplicationException;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.quartz.entity.JobItem;
import rs.formuvia.utils.StaticData;

public class JobDelete implements ExecuteQuery<Void> {
	private final UUID id;

	private DatabaseService databaseService = new DatabaseServiceImpl();
	private Logger logger = LogManager.getLogger(getClass());

	public JobDelete(UUID id) {
		this.id = id;
	}

	@Override
	public Void execute(Connection connection) throws Exception {
		JobItem jobItem = databaseService.findById(id, JobItem.class, connection);
		this.databaseService.delete(jobItem);
		deleteJob(jobItem);
		return null;
	}

	private void deleteJob(JobItem jobItem) {
		if (StaticData.databaseScheduler == null)
			return;
		JobDetail jobDetail = JobItemUpdate.createJobDetail(jobItem.getName(), jobItem.getReport().getId());

		try {
			if (StaticData.databaseScheduler.checkExists(jobDetail.getKey()))
				StaticData.databaseScheduler.deleteJob(jobDetail.getKey());
		} catch (SchedulerException e) {
			logger.error(e.getMessage(), e);
			throw new WebApplicationException(e);
		}
	}

}
