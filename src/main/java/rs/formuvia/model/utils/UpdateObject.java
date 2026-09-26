package rs.formuvia.model.utils;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import rs.formuvia.database.utils.LoadStaticData;
import rs.formuvia.model.dto.ModelColumnDTO;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.enums.ModelType;
import rs.formuvia.model.service.impl.ModelPreviewServiceImpl;
import rs.formuvia.utils.StringUtils;

public class UpdateObject implements ExecuteQuery<LinkedHashMap<String, Object>> {
	private final HttpServletRequest httpServletRequest;
	private final LinkedHashMap<String, Object> object;
	private final UUID modelId;

	private SqlQueryWriterService sqlQueryWriterService = new SqlQueryWriterServiceImpl();
	private CommonService commonService;
	private DatabaseService databaseService = new DatabaseServiceImpl();
	private ResourceBundleService resourceBundleService;

	public UpdateObject(HttpServletRequest httpServletRequest, LinkedHashMap<String, Object> object, UUID modelId) {
		this.httpServletRequest = httpServletRequest;
		this.object = object;
		this.modelId = modelId;
		this.commonService = new CommonServiceImpl(this.httpServletRequest);
		this.resourceBundleService = new ResourceBundleServiceImpl(this.httpServletRequest);
	}

	@Override
	public LinkedHashMap<String, Object> execute(Connection connection) throws Exception {
		ModelDTO modelDTO = ModelPreviewServiceImpl.findModel(modelId);
		List<ModelColumnDTO> columns = findColumns(modelDTO);

		Boolean insert = true;

		if (StringUtils.notNull(object.get(SqlQueryWriterServiceImpl.defaultIdColumn))) {
			insert = false;
			CreateForm.findObjectById(UUID.fromString(object.get(SqlQueryWriterServiceImpl.defaultIdColumn).toString()),
					modelDTO, connection, this.resourceBundleService);
		}

		String query = null;
		if (insert) {
			commonService.checkRole(modelDTO.getAddRoleCode());
			query = sqlQueryWriterService.insertQuery(columnsToArray(columns), modelDTO.getCode());
		} else {
			commonService.checkRole(modelDTO.getUpdateRoleCode());
			query = sqlQueryWriterService.updateQuery(columnsToArray(columns), modelDTO.getCode());
		}
		Map<Integer, Object> parameters = createParameter(object, columns);

		Object[] result = databaseService.executeNativeQuery(query, parameters, Object[].class, connection).getFirst();

		return createMapFromArray(result, columns);
	}

	public static List<ModelColumnDTO> findColumns(ModelDTO modelDTO) {
		List<ModelColumnDTO> list = new ArrayList<>();
		list.add(ModelColumnDTO.valueOf(SqlQueryWriterServiceImpl.defaultIdColumn, ColumnType.UUID));

		if (tableHasParent(modelDTO)) {
			list.add(ModelColumnDTO.valueOf(UpdateModel.PARENT_COLUMN_NAME, ColumnType.UUID));
		}

		list.addAll(LoadStaticData.findColumnsByModelId(modelDTO.getId()));

		return list;

	}

	private String[] columnsToArray(List<ModelColumnDTO> list) {
		return list.stream().map(a -> a.getCode()).toArray(String[]::new);
	}

	public static Boolean tableHasParent(ModelDTO modelDTO) {
		ModelDTO parentModel = ModelPreviewServiceImpl.findModel(modelDTO.getParentId());

		return parentModel.getType().equals(ModelType.TABLE);
	}

	private Map<Integer, Object> createParameter(LinkedHashMap<String, Object> object, List<ModelColumnDTO> columns) {
		Map<Integer, Object> parameters = new HashMap<>();
		int index = 0;
		for (ModelColumnDTO modelColumnDTO : columns) {
			if (modelColumnDTO.getCode().equals(SqlQueryWriterServiceImpl.defaultIdColumn)) {
				continue;
			}
			index++;
			Object value = object.get(modelColumnDTO.getCode());
			if (StringUtils.isNull(value)) {
				parameters.put(index, null);
				continue;
			}

			switch (modelColumnDTO.getColumnType()) {
			case BIGDECIMAL:
				parameters.put(index, new BigDecimal(value.toString()));
				break;
			case BOOLEAN:
				parameters.put(index, Boolean.valueOf(value.toString()));
				break;
			case INTEGER:
				parameters.put(index, new BigDecimal(value.toString()).intValue());
				break;
			case LOCALDATE:
				parameters.put(index, LocalDate.parse(value.toString()));
				break;
			case LOCALDATETIME:
				parameters.put(index, LocalDateTime.parse(value.toString()));
				break;
			case LOCALTIME:
				parameters.put(index, LocalTime.parse(value.toString()));
				break;
			case LONG:
				parameters.put(index, new BigDecimal(value.toString()).longValue());
				break;
			case STRING:
				parameters.put(index, value.toString());
				break;
			case UUID:
				parameters.put(index, UUID.fromString(value.toString()));
				break;

			}
		}

		if (StringUtils.notNull(object.get(SqlQueryWriterServiceImpl.defaultIdColumn))) {
			index++;
			parameters.put(index, UUID.fromString(object.get(SqlQueryWriterServiceImpl.defaultIdColumn).toString()));
		}
		return parameters;
	}

	private LinkedHashMap<String, Object> createMapFromArray(Object[] objects, List<ModelColumnDTO> columns) {
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();

		int index = -1;

		for (ModelColumnDTO column : columns) {
			index++;
			result.put(column.getCode(), objects[index]);
		}

		return result;
	}
}
