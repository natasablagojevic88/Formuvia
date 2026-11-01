package rs.formuvia.common.service;

import java.util.List;
import java.util.UUID;

import rs.formuvia.common.dto.JobListDTO;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.quartz.dto.JobLogDTO;

public interface JobService {

	List<JobListDTO> getList(UUID id);

	JobListDTO getJob(UUID id);

	void getExecute(UUID id);

	DatabaseTable<JobLogDTO> getJobLogs(UUID id, DatabaseParameter databaseParameter);

}
