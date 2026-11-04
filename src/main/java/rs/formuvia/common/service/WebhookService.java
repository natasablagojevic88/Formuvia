package rs.formuvia.common.service;

import java.util.UUID;

import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.quartz.dto.JobItemWebhookDTO;

public interface WebhookService {

	DatabaseTable<JobItemWebhookDTO> getWebhookTable(DatabaseParameter databaseParameter, UUID jobId);

	String getRandomToken();

	JobItemWebhookDTO getUpdate(JobItemWebhookDTO itemWebhookDTO);

	void getDelete(UUID id);

}
