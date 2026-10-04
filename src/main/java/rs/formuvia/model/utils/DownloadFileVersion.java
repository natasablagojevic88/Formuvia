package rs.formuvia.model.utils;

import java.net.HttpURLConnection;
import java.sql.Connection;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.entity.ModelFile;
import rs.formuvia.model.entity.ModelFileVersion;
import rs.formuvia.model.service.impl.ModelPreviewServiceImpl;
import rs.formuvia.utils.StringUtils;

public class DownloadFileVersion implements ExecuteQuery<Response> {
	private final HttpServletRequest httpServletRequest;
	private final UUID modelId;
	private final UUID id;
	private final String columnName;
	private final UUID versionId;
	private CommonService commonService;
	private static DatabaseService databaseService = new DatabaseServiceImpl();

	public DownloadFileVersion(HttpServletRequest httpServletRequest, UUID modelId, UUID id, String columnName,
			UUID versionId) {
		this.httpServletRequest = httpServletRequest;
		this.modelId = modelId;
		this.id = id;
		this.columnName = columnName;
		this.versionId = versionId;
		this.commonService = new CommonServiceImpl(this.httpServletRequest);
	}

	@Override
	public Response execute(Connection connection) throws Exception {
		ModelDTO modelDTO = ModelPreviewServiceImpl.findModel(modelId);
		commonService.checkRole(modelDTO.getPreviewRoleCode());
		ModelFileVersion modelFileVersion = findModelFileVersion(modelId, commonService, columnName, id, connection,
				versionId, modelDTO);

		return DownloadModelFile.createDownloadResponse(modelFileVersion.getPath(), modelFileVersion.getFileName(),
				modelFileVersion.getMimeType(), versionId);
	}

	public static ModelFileVersion findModelFileVersion(UUID modelId, CommonService commonService, String columnName,
			UUID id, Connection connection, UUID versionId, ModelDTO modelDTO) {

		ModelFile modelFile = DownloadModelFile.findModelFile(columnName, modelDTO, id, connection);

		if (StringUtils.isNull(modelFile))
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noFileFound", id);

		ModelFileVersion modelFileVersion = databaseService.findById(versionId, ModelFileVersion.class, connection);
		if (!modelFileVersion.getModelFile().getId().equals(modelFile.getId())) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "wrongParentFileModel", versionId);
		}

		return modelFileVersion;
	}

}
