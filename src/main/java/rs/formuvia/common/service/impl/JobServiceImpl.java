package rs.formuvia.common.service.impl;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jvnet.hk2.annotations.Service;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import rs.formuvia.common.dto.JobListDTO;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.JobService;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.quartz.dto.JobItemDTO;
import rs.formuvia.quartz.dto.JobLogDTO;
import rs.formuvia.quartz.utils.JobItemUpdate;
import rs.formuvia.utils.StaticData;

@Service
public class JobServiceImpl implements JobService {

	@Inject
	private CommonService commonService;

	private DatabaseService databaseService = new DatabaseServiceImpl();

	@Override
	public List<JobListDTO> getList(UUID id) {
		Map<Integer, Object> parameters = Map.of(1, JobItemUpdate.REPORT_QUARTZ_GROUP, 2,
				commonService.getRoles().stream().toArray(String[]::new), 3, id == null ? "%" : id.toString());

		return databaseService.executeNativeQuery(commonService.readQueryFromFile("listJobs.sql"), parameters,
				JobListDTO.class);
	}

	@Override
	public JobListDTO getJob(UUID id) {
		List<JobListDTO> list = getList(id);

		if (list.isEmpty())
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noJobFound", id);
		return list.getFirst();
	}

	@Override
	public void getExecute(UUID id) {
		getJob(id);
		JobItemDTO jobItemDTO = this.databaseService.findById(id, JobItemDTO.class);
		JobDetail jobDetail = JobItemUpdate.createJobDetail(jobItemDTO.getName(), jobItemDTO.getReportId(), id);

		if (StaticData.databaseScheduler == null)
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noSchedulerFound", null);

		try {
			if (!StaticData.databaseScheduler.checkExists(jobDetail.getKey()))
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noJobFound", id);
		} catch (SchedulerException e) {
			throw new WebApplicationException(e);
		}

		try {
			StaticData.databaseScheduler.triggerJob(jobDetail.getKey());
		} catch (SchedulerException e) {
			throw new WebApplicationException(e);
		}

	}

	@Override
	public DatabaseTable<JobLogDTO> getJobLogs(UUID id, DatabaseParameter databaseParameter) {
		getJob(id);
		databaseParameter.getFilters().add(DatabaseFilter.valueOf("jobItemId", id.toString()));
		return this.databaseService.createTable(databaseParameter, JobLogDTO.class);
	}

}
