package rs.formuvia.model.utils;

import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.model.dto.ModelColumnDTO;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.service.impl.ModelPreviewServiceImpl;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

public class CreateColumnChange implements ExecuteQuery<LinkedHashMap<String, Object>> {
	private final HttpServletRequest httpServletRequest;
	private final LinkedHashMap<String, Object> object;
	private final UUID modelId;
	private final String columnName;

	private CommonService commonService;
	private DatabaseService databaseService = new DatabaseServiceImpl();

	private final String FUNCTION_NAME_TO_REPLACE = "#function_name#";
	private final String FUNCTION_QUERY = "SELECT * FROM #function_name#(?::json)";

	public CreateColumnChange(HttpServletRequest httpServletRequest, LinkedHashMap<String, Object> object, UUID modelId,
			String columnName) {
		this.httpServletRequest = httpServletRequest;
		this.object = object;
		this.modelId = modelId;
		this.columnName = columnName;
		this.commonService = new CommonServiceImpl(this.httpServletRequest);
	}

	@SuppressWarnings("unchecked")
	@Override
	public LinkedHashMap<String, Object> execute(Connection connection) throws Exception {
		ModelDTO modelDTO = ModelPreviewServiceImpl.findModel(modelId);
		this.commonService.checkRole(modelDTO.getUpdateRoleCode());

		ModelColumnDTO modelColumnDTO = StaticData.modelColumns.stream()
				.filter(a -> a.getModelId().equals(this.modelId)).filter(a -> a.getCode().equals(this.columnName))
				.filter(a -> StringUtils.hasText(a.getChangeValueFunction())).findFirst().orElseThrow(
						() -> new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noChangeColumn", columnName));
		String query = FUNCTION_QUERY.replace(this.FUNCTION_NAME_TO_REPLACE, modelColumnDTO.getChangeValueFunction());
		String jsonText = StaticData.JSONB.toJson(this.object);
		Map<Integer, Object> parameters = Map.of(1, jsonText);
		List<Object[]> objects = databaseService.executeNativeQuery(query, parameters, Object[].class, connection);
		if (objects.isEmpty()) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "functionHasNotFinished",
					modelColumnDTO.getChangeValueFunction());
		}
		String response = objects.getFirst()[0].toString();
		;
		return StaticData.JSONB.fromJson(new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)),
				LinkedHashMap.class);
	}

}
