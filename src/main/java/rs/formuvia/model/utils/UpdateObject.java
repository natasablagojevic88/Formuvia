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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import rs.formuvia.model.dto.ModelColumnDTO;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.dto.ModelFileVersionDTO;
import rs.formuvia.model.entity.ModelFile;
import rs.formuvia.model.entity.ModelFileVersion;
import rs.formuvia.model.enums.ModelType;
import rs.formuvia.model.service.impl.ModelPreviewServiceImpl;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

public class UpdateObject implements ExecuteQuery<LinkedHashMap<String, Object>> {
	private final HttpServletRequest httpServletRequest;
	private final LinkedHashMap<String, Object> object;
	private final UUID modelId;

	private SqlQueryWriterService sqlQueryWriterService = new SqlQueryWriterServiceImpl();
	private CommonService commonService;
	private DatabaseService databaseService = new DatabaseServiceImpl();
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

		String query = null;
		if (insert) {
			commonService.checkRole(modelDTO.getAddRoleCode());
			query = sqlQueryWriterService.insertQuery(columnsToArray(columns), modelDTO.getCode());
		} else {
			commonService.checkRole(modelDTO.getUpdateRoleCode());
			query = sqlQueryWriterService.updateQuery(columnsToArray(columns), modelDTO.getCode());
		}
		Map<Integer, Object> parameters = createParameter(modelDTO, object, columns, connection);

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

	@SuppressWarnings("unchecked")
	private Map<Integer, Object> createParameter(ModelDTO modelDTO, LinkedHashMap<String, Object> object,
			List<ModelColumnDTO> columns, Connection connection) {
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
			case FILE: {
				Map<String, Object> fileObject = (Map<String, Object>) value;
				UUID rowId = StringUtils.isNull(object.get(SqlQueryWriterServiceImpl.defaultIdColumn)) ? null
						: UUID.fromString(object.get(SqlQueryWriterServiceImpl.defaultIdColumn).toString());
				parameters.put(index, createFile(fileObject, rowId, connection, modelDTO, modelColumnDTO.getCode()));
				break;
			}
			}
		}

		if (StringUtils.notNull(object.get(SqlQueryWriterServiceImpl.defaultIdColumn))) {
			index++;
			parameters.put(index, UUID.fromString(object.get(SqlQueryWriterServiceImpl.defaultIdColumn).toString()));
		}
		return parameters;
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

		FileUpload fileUpload = this.databaseService
				.findById(UUID.fromString(fileObject.get("fileUploadFile").toString()), FileUpload.class, connection);

		if (!fileUpload.getAppUser().getId().equals(commonService.getUser().getId())) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "wrongUser", fileUpload.getId());
		}

		File rootFile = new File(StaticData.appProperties.getProperty(FileUploadServiceImpl.PATH_FILE_PARAMETER));
		File file = new File(rootFile.getAbsolutePath() + fileUpload.getPath());

		if (!file.exists()) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noFile", fileUpload.getPath());
		}

		String mimeType = null;

		try {
			mimeType = tika.detect(file);
		} catch (IOException e) {
			throw new WebApplicationException(e);
		}

		ModelFile modelFileFromBase = rowId == null ? null
				: DownloadModelFile.findModelFile(columnName, modelDTO, rowId, connection);

		ModelFile modelFile = modelFileFromBase == null ? new ModelFile() : modelFileFromBase;
		modelFile.setFileName(fileName);
		modelFile.setMimeType(mimeType);
		modelFile.setPath(fileUpload.getPath());

		modelFile = this.databaseService.save(modelFile, connection);

		DatabaseParameter databaseParameter = DatabaseParameter
				.valueOf(DatabaseFilter.valueOf("modelFileId", modelFile.getId().toString()));
		databaseParameter.getOrders().add(QueryDatabaseOrder.valueOf("version", Direction.DESC));
		databaseParameter.setPageSize(1);

		List<ModelFileVersionDTO> modelFileVersionDTOs = this.databaseService.findAll(databaseParameter,
				ModelFileVersionDTO.class, connection);
		Integer version = 1;

		if (!modelFileVersionDTOs.isEmpty())
			version = modelFileVersionDTOs.getFirst().getVersion() + 1;

		ModelFileVersion modelFileVersion = new ModelFileVersion();
		modelFileVersion.setFileName(modelFile.getFileName());
		modelFileVersion.setMimeType(modelFile.getMimeType());
		modelFileVersion.setModelFile(modelFile);
		modelFileVersion.setPath(modelFile.getPath());
		modelFileVersion.setVersion(version);
		this.databaseService.save(modelFileVersion, connection);

		return modelFile.getId();
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
