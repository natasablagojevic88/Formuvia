package rs.formuvia.common.service.impl;

import java.util.UUID;

import org.jvnet.hk2.annotations.Service;
import org.quartz.JobExecutionException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import rs.formuvia.common.service.ExecuteWebhookService;
import rs.formuvia.exceptions.UnAuthorizedException;
import rs.formuvia.quartz.dto.JobItemWebhookDTO;
import rs.formuvia.quartz.jobs.ReportJob;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

@Service
public class ExecuteWebhookServiceImpl implements ExecuteWebhookService {

	@Context
	private HttpServletRequest httpServletRequest;

	private final String authHeader = "Authorization";
	private final String authHeaderPrefix = "Bearer ";

	@Override
	public void getExecute(UUID id) {
		JobItemWebhookDTO jobItemWebhookDTO = StaticData.webhooks.stream().filter(a -> a.getId().equals(id)).findFirst()
				.get();

		if (!StringUtils.hasText(this.httpServletRequest.getHeader(authHeader)))
			throw new UnAuthorizedException();

		if (!this.httpServletRequest.getHeader(authHeader).startsWith(authHeaderPrefix))
			throw new UnAuthorizedException();

		if (!this.httpServletRequest.getHeader(authHeader).substring(authHeaderPrefix.length())
				.equals(jobItemWebhookDTO.getBearerToken()))
			throw new UnAuthorizedException();

		ReportJob reportJob = new ReportJob(jobItemWebhookDTO.getJobId(), jobItemWebhookDTO.getJobReportId());
		try {
			reportJob.execute(null);
		} catch (JobExecutionException e) {
			throw new WebApplicationException(e);
		}
	}

}
