package rs.formuvia.model.utils;

import java.io.File;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.common.service.impl.ExportTableServiceImpl;
import rs.formuvia.common.service.impl.FileUploadServiceImpl;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.entity.ModelFile;
import rs.formuvia.model.service.impl.ModelPreviewServiceImpl;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

public class DownloadModelFile implements ExecuteQuery<Response> {
	private final HttpServletRequest httpServletRequest;
	private final UUID modelId;
	private final UUID id;
	private final String columnName;
	private static final String COLUMN_TO_REPLACE = "#column_name#";
	private static final String TABLE_TO_REPLACE = "#table_name#";
	private static final String QUERY = "select #column_name# from #table_name# where id=?";

	private CommonService commonService;
	private static DatabaseService databaseService = new DatabaseServiceImpl();

	public DownloadModelFile(HttpServletRequest httpServletRequest, UUID modelId, UUID id, String columnName) {
		this.httpServletRequest = httpServletRequest;
		this.modelId = modelId;
		this.id = id;
		this.columnName = columnName;
		this.commonService = new CommonServiceImpl(this.httpServletRequest);
	}

	@Override
	public Response execute(Connection connection) throws Exception {
		ModelDTO modelDTO = ModelPreviewServiceImpl.findModel(modelId);
		this.commonService.checkRole(modelDTO.getPreviewRoleCode());

		ModelFile modelFile = findModelFile(columnName, modelDTO, id, connection);

		if (StringUtils.isNull(modelFile))
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noFile", id);

		File rootFile = new File(StaticData.appProperties.getProperty(FileUploadServiceImpl.PATH_FILE_PARAMETER));
		File file = new File(rootFile.getAbsolutePath() + "/" + modelFile.getPath());
		if (!file.exists()) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noFile", id);
		}

		return ExportTableServiceImpl.createExcelResponse(Files.readAllBytes(Paths.get(file.getAbsolutePath())),
				modelFile.getFileName(), modelFile.getMimeType());
	}

	public static ModelFile findModelFile(String columnName, ModelDTO modelDTO, UUID rowId, Connection connection) {
		String columnNameBase = StaticData.modelColumns.stream().filter(a -> a.getModelId().equals(modelDTO.getId()))
				.filter(a -> a.getCode().equals(columnName)).findFirst()
				.orElseThrow(() -> new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noColumn", columnName))
				.getCode();

		String query = QUERY.replace(TABLE_TO_REPLACE, modelDTO.getCode());
		query = query.replace(COLUMN_TO_REPLACE, columnNameBase);

		Map<Integer, Object> parameters = Map.of(1, rowId);

		List<UUID> result = databaseService.executeNativeQuery(query, parameters, UUID.class, connection);

		if (result.isEmpty())
			return null;
		else if (StringUtils.isNull(result.getFirst()))
			return null;
		else
			return databaseService.findById(result.getFirst(), ModelFile.class, connection);

	}

}
