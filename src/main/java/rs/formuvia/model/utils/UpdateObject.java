package rs.formuvia.model.utils;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.tika.Tika;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import rs.formuvia.common.entity.FileUpload;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.ResourceBundleService;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.common.service.impl.FileUploadServiceImpl;
import rs.formuvia.common.service.impl.ResourceBundleServiceImpl;
import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.database.enums.Direction;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.SqlQueryWriterService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.service.impl.SqlQueryWriterServiceImpl;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.database.utils.LoadStaticData;
import rs.formuvia.database.utils.QueryDatabaseOrder;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.model.dto.ModelColumnConditionDTO;
import rs.formuvia.model.dto.ModelColumnDTO;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.dto.ModelFileVersionDTO;
import rs.formuvia.model.entity.ModelFile;
import rs.formuvia.model.entity.ModelFileVersion;
import rs.formuvia.model.enums.ModelColumnConditionType;
import rs.formuvia.model.enums.ModelType;
import rs.formuvia.model.service.impl.ModelPreviewServiceImpl;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

public class UpdateObject implements ExecuteQuery<LinkedHashMap<String, Object>> {
	private final HttpServletRequest httpServletRequest;
	private LinkedHashMap<String, Object> object;
	private final UUID modelId;

	private SqlQueryWriterService sqlQueryWriterService = new SqlQueryWriterServiceImpl();
	private CommonService commonService;
	private static DatabaseService databaseService = new DatabaseServiceImpl();
	private ResourceBundleService resourceBundleService;
	private static Tika tika = new Tika();

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
		checkObject(columns, object, resourceBundleService);
		Boolean insert = true;

		if (StringUtils.notNull(object.get(SqlQueryWriterServiceImpl.defaultIdColumn))) {
			insert = false;
			LinkedHashMap<String, Object> current = CreateForm.findObjectById(
					UUID.fromString(object.get(SqlQueryWriterServiceImpl.defaultIdColumn).toString()), modelDTO,
					connection, this.resourceBundleService);
			if (StringUtils.notNull(current.get(UpdateModel.PARENT_COLUMN_NAME))) {
				object.put(UpdateModel.PARENT_COLUMN_NAME, current.get(UpdateModel.PARENT_COLUMN_NAME));
			}
		}
		transformValues(modelDTO, object, columns, connection);
		checkConditions(object, columns, connection);

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

	private void checkConditions(LinkedHashMap<String, Object> object, List<ModelColumnDTO> columns,
			Connection connection) {
		for (ModelColumnDTO column : columns.stream().filter(a -> StringUtils.notNull(a.getId()))
				.sorted(Comparator.comparing(ModelColumnDTO::getRowIndex).thenComparing(ModelColumnDTO::getColumnIndex))
				.toList()) {
			if (!columnHasCondition(column.getId())) {
				continue;
			}

			List<ModelColumnConditionDTO> showableCondition = conditionForColumn(column.getId(),
					ModelColumnConditionType.SHOWABLE);
			if (!checkCondition(showableCondition, connection, object)) {
				object.put(column.getCode(), null);
				continue;
			}

			if (StringUtils.isNull(object.get(column.getCode()))) {
				List<ModelColumnConditionDTO> notNullCondition = conditionForColumn(column.getId(),
						ModelColumnConditionType.REQUIRED);
				if (notNullCondition.isEmpty()) {
					continue;
				}
				if (checkCondition(notNullCondition, connection, object))
					throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "fieldRequired",
							this.resourceBundleService.getText(column.getName()));
			}

		}

	}

	private Boolean checkCondition(List<ModelColumnConditionDTO> list, Connection connection,
			LinkedHashMap<String, Object> object) {
		for (ModelColumnConditionDTO columnCondition : list) {
			switch (columnCondition.getSearchOperation()) {
			case IS_NULL: {
				if (StringUtils.notNull(object.get(columnCondition.getConditionColumnCode())))
					return false;
				break;
			}
			case IS_NOT_NULL: {
				if (StringUtils.isNull(object.get(columnCondition.getConditionColumnCode())))
					return false;
				break;
			}
			default: {
				String query = "SELECT "
						+ SqlQueryWriterServiceImpl.conditionString("?", columnCondition.getSearchOperation());
				Map<Integer, Object> parameters = new HashMap<>();
				int index = 1;
				parameters.put(index, object.get(columnCondition.getConditionColumnCode()));
				DatabaseFilter databaseFilter = new DatabaseFilter(columnCondition.getConditionColumnCode(),
						columnCondition.getSearchOperation(), columnCondition.getConditionColumnColumnType(),
						columnCondition.getField1(), columnCondition.getField2());
				Object[] objects = SqlQueryWriterServiceImpl.createParametersForFilters(databaseFilter);
				for (Object objectValue : objects) {
					index++;
					parameters.put(index, objectValue);
				}
				Boolean checkValue = databaseService.executeNativeQuery(query, parameters, Boolean.class, connection)
						.getFirst();
				if (StringUtils.isNull(checkValue) || (!checkValue))
					return false;
				break;
			}
			}

		}

		return true;

	}

	private Boolean columnHasCondition(UUID columnId) {
		return StaticData.modelColumnsConditions.stream().filter(a -> a.getModelColumnId().equals(columnId))
				.count() > 0;
	}

	private static List<ModelColumnConditionDTO> conditionForColumn(UUID columnId, ModelColumnConditionType type) {
		return StaticData.modelColumnsConditions.stream().filter(a -> a.getModelColumnId().equals(columnId))
				.filter(a -> a.getType().equals(type)).collect(Collectors.toList());
	}

	public static Boolean columnHasConditionByType(UUID columnId, ModelColumnConditionType type) {
		return !conditionForColumn(columnId, type).isEmpty();
	}

	private Map<Integer, Object> createParameter(LinkedHashMap<String, Object> object, List<ModelColumnDTO> columns) {
		Map<Integer, Object> parameters = new HashMap<>();
		int index = 0;
		for (ModelColumnDTO column : columns) {
			if (column.getCode().equals(SqlQueryWriterServiceImpl.defaultIdColumn)) {
				continue;
			}
			index++;
			parameters.put(index, object.get(column.getCode()));

		}

		if (StringUtils.notNull(object.get(SqlQueryWriterServiceImpl.defaultIdColumn))) {
			index++;
			parameters.put(index, object.get(SqlQueryWriterServiceImpl.defaultIdColumn));
		}
		return parameters;
	}

	private void checkObject(List<ModelColumnDTO> columns, LinkedHashMap<String, Object> object,
			ResourceBundleService resourceBundleService) {
		for (ModelColumnDTO columnDTO : columns.stream().filter(a -> StringUtils.notNull(a.getNullable()))
				.filter(a -> !a.getNullable()).collect(Collectors.toList())) {
			if (StringUtils.notNull(object.get(columnDTO.getCode()))) {
				continue;
			}

			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "fieldRequired",
					this.resourceBundleService.getText(columnDTO.getName()));
		}
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

	private void transformValues(ModelDTO modelDTO, LinkedHashMap<String, Object> object, List<ModelColumnDTO> columns,
			Connection connection) {
		for (ModelColumnDTO modelColumnDTO : columns) {
			if (modelColumnDTO.getCode().equals(SqlQueryWriterServiceImpl.defaultIdColumn)) {
				continue;
			}
			String columnCode = modelColumnDTO.getCode();
			object.put(columnCode, createValue(object.get(columnCode), modelColumnDTO, modelDTO, connection));

		}

		if (StringUtils.notNull(object.get(SqlQueryWriterServiceImpl.defaultIdColumn))) {
			object.put(SqlQueryWriterServiceImpl.defaultIdColumn,
					UUID.fromString(object.get(SqlQueryWriterServiceImpl.defaultIdColumn).toString()));
		}
	}

	@SuppressWarnings("unchecked")
	private Object createValue(Object value, ModelColumnDTO modelColumnDTO, ModelDTO modelDTO, Connection connection) {

		if (StringUtils.isNull(value)) {
			return null;
		}

		switch (modelColumnDTO.getColumnType()) {
		case BIGDECIMAL:
			return new BigDecimal(value.toString());
		case BOOLEAN:
			return Boolean.valueOf(value.toString());
		case INTEGER:
			return new BigDecimal(value.toString()).intValue();
		case LOCALDATE:
			return LocalDate.parse(value.toString());
		case LOCALDATETIME:
			return LocalDateTime.parse(value.toString());
		case LOCALTIME:
			return LocalTime.parse(value.toString());
		case LONG:
			return new BigDecimal(value.toString()).longValue();
		case STRING:
			return value.toString();
		case UUID:
			return UUID.fromString(value.toString());
		case FILE: {
			Map<String, Object> fileObject = (Map<String, Object>) value;
			UUID rowId = StringUtils.isNull(object.get(SqlQueryWriterServiceImpl.defaultIdColumn)) ? null
					: UUID.fromString(object.get(SqlQueryWriterServiceImpl.defaultIdColumn).toString());
			return createFile(fileObject, rowId, connection, modelDTO, modelColumnDTO.getCode());
		}
		}

		return null;
	}

	private UUID createFile(Map<String, Object> fileObject, UUID rowId, Connection connection, ModelDTO modelDTO,
			String columnName) {
		if (StringUtils.isNull(fileObject.get("fileUploadFile"))) {

			if (StringUtils.isNull(fileObject.get("id")))
				return null;
			else
				return UUID.fromString(fileObject.get("id").toString());
		}

		String fileName = (String) fileObject.get("fileName");

		UUID fileId = UUID.fromString(fileObject.get("fileUploadFile").toString());
		FileUpload fileUpload = databaseService.findById(fileId, FileUpload.class, connection);
		File file = createUploadFile(fileUpload, connection, commonService);

		String mimeType = findMimeType(file);
		ModelFile modelFileFromBase = rowId == null ? null
				: DownloadModelFile.findModelFile(columnName, modelDTO, rowId, connection);

		ModelFile modelFile = modelFileFromBase == null ? new ModelFile() : modelFileFromBase;
		modelFile.setFileName(fileName);
		modelFile.setMimeType(mimeType);
		modelFile.setPath(fileUpload.getPath());

		modelFile = databaseService.save(modelFile, connection);

		Integer version = 1;

		List<ModelFileVersionDTO> modelFileVersionDTOs = findLastestFileVersion(modelFile.getId(), connection);

		if (!modelFileVersionDTOs.isEmpty())
			version = modelFileVersionDTOs.getFirst().getVersion() + 1;

		ModelFileVersion modelFileVersion = new ModelFileVersion();
		modelFileVersion.setFileName(modelFile.getFileName());
		modelFileVersion.setMimeType(modelFile.getMimeType());
		modelFileVersion.setModelFile(modelFile);
		modelFileVersion.setPath(modelFile.getPath());
		modelFileVersion.setVersion(version);
		modelFileVersion.setCreationDate(LocalDateTime.now());
		databaseService.save(modelFileVersion, connection);

		return modelFile.getId();
	}

	public static List<ModelFileVersionDTO> findLastestFileVersion(UUID modellFileId, Connection connection) {
		DatabaseParameter databaseParameter = DatabaseParameter
				.valueOf(DatabaseFilter.valueOf("modelFileId", modellFileId.toString()));
		databaseParameter.getOrders().add(QueryDatabaseOrder.valueOf("version", Direction.DESC));
		databaseParameter.setPageSize(1);

		return databaseService.findAll(databaseParameter, ModelFileVersionDTO.class, connection);

	}

	public static String findMimeType(File file) {
		String mimeType = null;

		try {
			mimeType = tika.detect(file);
		} catch (IOException e) {
			throw new WebApplicationException(e);
		}
		return mimeType;
	}

	public static File createUploadFile(FileUpload fileUpload, Connection connection, CommonService commonService) {

		if (!fileUpload.getAppUser().getId().equals(commonService.getUser().getId())) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "wrongUser", fileUpload.getId());
		}

		File rootFile = new File(StaticData.appProperties.getProperty(FileUploadServiceImpl.PATH_FILE_PARAMETER));
		File file = new File(rootFile.getAbsolutePath() + fileUpload.getPath());

		if (!file.exists()) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noFile", fileUpload.getPath());
		}
		return file;
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
