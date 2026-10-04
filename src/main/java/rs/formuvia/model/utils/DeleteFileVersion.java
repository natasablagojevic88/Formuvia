package rs.formuvia.model.utils;

import java.net.HttpURLConnection;
import java.sql.Connection;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.database.enums.SearchOperation;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.dto.ModelFileVersionDTO;
import rs.formuvia.model.entity.ModelFile;
import rs.formuvia.model.entity.ModelFileVersion;
import rs.formuvia.model.service.impl.ModelPreviewServiceImpl;

public class DeleteFileVersion implements ExecuteQuery<Void> {
	private final HttpServletRequest httpServletRequest;
	private final UUID modelId;
	private final UUID id;
	private final String columnName;
	private final UUID versionId;
	private CommonService commonService;
	private DatabaseService databaseService = new DatabaseServiceImpl();

	public DeleteFileVersion(HttpServletRequest httpServletRequest, UUID modelId, UUID id, String columnName,
			UUID versionId) {
		this.httpServletRequest = httpServletRequest;
		this.modelId = modelId;
		this.id = id;
		this.columnName = columnName;
		this.versionId = versionId;
		this.commonService = new CommonServiceImpl(this.httpServletRequest);
	}

	@Override
	public Void execute(Connection connection) throws Exception {
		ModelDTO modelDTO = ModelPreviewServiceImpl.findModel(modelId);
		commonService.checkRole(modelDTO.getDeleteRoleCode());
		ModelFileVersion modelFileVersion = DownloadFileVersion.findModelFileVersion(modelId, commonService, columnName,
				id, connection, versionId, modelDTO);
		checkOnlyVersion(connection, modelFileVersion);

		this.databaseService.delete(modelFileVersion, connection);

		if (modelFileVersion.getModelFile().getPath().equals(modelFileVersion.getPath())) {
			List<ModelFileVersionDTO> modelFileVersionDTOs = UpdateObject
					.findLastestFileVersion(modelFileVersion.getModelFile().getId(), connection);
			if (modelFileVersionDTOs.isEmpty()) {
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "cantDeleteOnlyVersion",
						modelFileVersion.getId());
			}
			ModelFile modelFile = modelFileVersion.getModelFile();
			ModelFileVersionDTO lastest = modelFileVersionDTOs.getFirst();
			modelFile.setFileName(lastest.getFileName());
			modelFile.setMimeType(lastest.getMimeType());
			modelFile.setPath(lastest.getPath());
			this.databaseService.save(modelFile, connection);
		}

		return null;
	}

	private void checkOnlyVersion(Connection connection, ModelFileVersion modelFileVersion) {
		DatabaseParameter databaseParameter = DatabaseParameter.valueOf(new DatabaseFilter[] {
				DatabaseFilter.valueOf("modelFile", modelFileVersion.getModelFile().getId().toString()),
				DatabaseFilter.valueOf("id", SearchOperation.NOT_EQUALS, modelFileVersion.getId().toString()) });

		if (!this.databaseService.exists(databaseParameter, ModelFileVersion.class, connection)) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "cantDeleteOnlyVersion",
					modelFileVersion.getId());
		}
	}

}
