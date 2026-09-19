package rs.formuvia.quartz.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;

public class ClearTokenJob implements Job {
	private final String CLEAR_TOKEN_QUERY = "delete_expired_token.sql";

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		DatabaseService databaseService = new DatabaseServiceImpl();
		CommonService commonService = new CommonServiceImpl();
		String deleteToken = commonService.readQueryFromFile(CLEAR_TOKEN_QUERY);
		databaseService.executeUpdateQuery(deleteToken, null);

	}

}
