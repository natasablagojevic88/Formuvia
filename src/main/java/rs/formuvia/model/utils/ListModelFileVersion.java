package rs.formuvia.model.utils;

import java.net.HttpURLConnection;
import java.sql.Connection;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.database.enums.Direction;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.database.utils.QueryDatabaseOrder;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.dto.ModelFileVersionDTO;
import rs.formuvia.model.entity.ModelFile;
import rs.formuvia.model.service.impl.ModelPreviewServiceImpl;
import rs.formuvia.utils.StringUtils;

public class ListModelFileVersion implements ExecuteQuery<DatabaseTable<ModelFileVersionDTO>> {
	private final HttpServletRequest httpServletRequest;
	private final UUID modelId;
	private final UUID id;
	private final String columnName;
	private CommonService commonService;
	private DatabaseService databaseService = new DatabaseServiceImpl();

	public ListModelFileVersion(HttpServletRequest httpServletRequest, UUID modelId, UUID id, String columnName) {
		this.httpServletRequest = httpServletRequest;
		this.modelId = modelId;
		this.id = id;
		this.columnName = columnName;
		this.commonService = new CommonServiceImpl(this.httpServletRequest);
	}

	@Override
	public DatabaseTable<ModelFileVersionDTO> execute(Connection connection) throws Exception {
		ModelDTO modelDTO = ModelPreviewServiceImpl.findModel(modelId);
		this.commonService.checkRole(modelDTO.getPreviewRoleCode());

		ModelFile modelFile = DownloadModelFile.findModelFile(columnName, modelDTO, id, connection);
		if (StringUtils.isNull(modelFile))
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noFileFound", id);
		DatabaseParameter databaseParameter = new DatabaseParameter();
		databaseParameter.getFilters().add(DatabaseFilter.valueOf("modelFileId", modelFile.getId().toString()));
		databaseParameter.getOrders().add(QueryDatabaseOrder.valueOf("version", Direction.DESC));

		return databaseService.createTable(databaseParameter, ModelFileVersionDTO.class, connection);
	}

}
