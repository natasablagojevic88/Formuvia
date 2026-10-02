package rs.formuvia.model.utils;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.ResourceBundleService;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.common.service.impl.ResourceBundleServiceImpl;
import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.SqlQueryWriterService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.service.impl.SqlQueryWriterServiceImpl;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.model.dto.ModelColumnDTO;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.entity.ModelFile;
import rs.formuvia.model.service.impl.ModelPreviewServiceImpl;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

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
		LinkedHashMap<String, Object> item = CreateForm.findObjectById(id, modelDTO, connection, resourceBundleService);

		for (ModelColumnDTO columnDTO : StaticData.modelColumns.stream()
				.filter(a -> a.getModelId().equals(this.modelId)).filter(a -> a.getColumnType().equals(ColumnType.FILE))
				.collect(Collectors.toList())) {
			if (StringUtils.isNull(item.get(columnDTO.getCode()))) {
				continue;
			}

			UUID fileRowId = UUID.fromString(item.get(columnDTO.getCode()).toString());

			ModelFile modelFile = this.databaseService.findById(fileRowId, ModelFile.class, connection);
			this.databaseService.delete(modelFile, connection);
		}

		String query = sqlQueryWriterService.deleteQuery(modelDTO.getCode());
		Map<Integer, Object> parameters = Map.of(1, this.id);

		databaseService.executeUpdateQuery(query, parameters, connection);

		return null;
	}

}
