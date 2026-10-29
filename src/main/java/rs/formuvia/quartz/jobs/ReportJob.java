package rs.formuvia.quartz.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import rs.formuvia.quartz.utils.JobItemUpdate;

public class ReportJob implements Job {

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		System.out.println("JOB RADI!" + context.getJobDetail().getJobDataMap().getString(JobItemUpdate.REPORT_ID));

	}

}
