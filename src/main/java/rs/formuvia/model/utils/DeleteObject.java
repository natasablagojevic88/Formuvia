package rs.formuvia.model.utils;

import java.sql.Connection;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.ResourceBundleService;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.common.service.impl.ResourceBundleServiceImpl;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.SqlQueryWriterService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.service.impl.SqlQueryWriterServiceImpl;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.service.impl.ModelPreviewServiceImpl;

public class DeleteObject implements ExecuteQuery<Void> {
	private final HttpServletRequest httpServletRequest;
	private final UUID modelId;
	private final UUID id;

	private DatabaseService databaseService = new DatabaseServiceImpl();
	private SqlQueryWriterService sqlQueryWriterService = new SqlQueryWriterServiceImpl();
	private CommonService commonService;
	private ResourceBundleService resourceBundleService;

	public DeleteObject(HttpServletRequest httpServletRequest, UUID modelId, UUID id) {
		this.httpServletRequest = httpServletRequest;
		this.modelId = modelId;
		this.id = id;
		this.commonService = new CommonServiceImpl(this.httpServletRequest);
		this.resourceBundleService = new ResourceBundleServiceImpl(this.httpServletRequest);
	}

	@Override
	public Void execute(Connection connection) throws Exception {
		ModelDTO modelDTO = ModelPreviewServiceImpl.findModel(modelId);
		this.commonService.checkRole(modelDTO.getDeleteRoleCode());
		CreateForm.findObjectById(id, modelDTO, connection, resourceBundleService);

		String query = sqlQueryWriterService.deleteQuery(modelDTO.getCode());
		Map<Integer, Object> parameters = Map.of(1, this.id);

		databaseService.executeUpdateQuery(query, parameters, connection);

		return null;
	}

}
