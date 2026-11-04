package rs.formuvia.common.service.impl;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import org.jvnet.hk2.annotations.Service;
import org.modelmapper.ModelMapper;

import jakarta.inject.Inject;
import rs.formuvia.common.service.WebhookService;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.quartz.dto.JobItemWebhookDTO;
import rs.formuvia.quartz.entity.JobItemWebhook;
import rs.formuvia.utils.StringUtils;

@Service
public class WebhookServiceImpl implements WebhookService {

	@Inject
	private DatabaseService databaseService;

	private ModelMapper modelMapper = new ModelMapper();

	@Override
	public DatabaseTable<JobItemWebhookDTO> getWebhookTable(DatabaseParameter databaseParameter, UUID jobId) {
		databaseParameter.getFilters().add(DatabaseFilter.valueOf("jobId", jobId.toString()));
		return databaseService.createTable(databaseParameter, JobItemWebhookDTO.class);
	}

	@Override
	public String getRandomToken() {
		byte[] bytes = new byte[32];
		new SecureRandom().nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	@Override
	public JobItemWebhookDTO getUpdate(JobItemWebhookDTO itemWebhookDTO) {
		JobItemWebhook jobItemWebhook = itemWebhookDTO.getId() == null ? new JobItemWebhook()
				: this.databaseService.findById(itemWebhookDTO.getId(), JobItemWebhook.class);

		if (StringUtils.isNull(itemWebhookDTO.getId()))
			itemWebhookDTO.setBearerToken(getRandomToken());
		else
			itemWebhookDTO.setBearerToken(jobItemWebhook.getBearerToken());

		this.modelMapper.map(itemWebhookDTO, jobItemWebhook);

		jobItemWebhook = this.databaseService.save(jobItemWebhook);

		return this.modelMapper.map(jobItemWebhook, JobItemWebhookDTO.class);
	}

	@Override
	public void getDelete(UUID id) {
		JobItemWebhook jobItemWebhook = this.databaseService.findById(id, JobItemWebhook.class);
		this.databaseService.delete(jobItemWebhook);

	}

}
