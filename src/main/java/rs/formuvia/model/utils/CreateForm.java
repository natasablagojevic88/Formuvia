package rs.formuvia.model.utils;

import java.net.HttpURLConnection;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import rs.formuvia.common.dto.ComboboxDTO;
import rs.formuvia.common.dto.FileUploadDTO;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.ResourceBundleService;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.common.service.impl.ResourceBundleServiceImpl;
import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.service.impl.SqlQueryWriterServiceImpl;
import rs.formuvia.database.utils.DatabaseColumn;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.database.utils.LoadStaticData;
import rs.formuvia.database.utils.ParentListOfValues;
import rs.formuvia.database.utils.QueryColumnInfo;
import rs.formuvia.database.utils.QueryTableInfo;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.model.dto.ModelColumnDTO;
import rs.formuvia.model.dto.ModelColumnPreviewDTO;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.dto.ModelFileDTO;
import rs.formuvia.model.dto.ObjectFormDTO;
import rs.formuvia.model.service.impl.ModelPreviewServiceImpl;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

public class CreateForm implements ExecuteQuery<ObjectFormDTO> {
	private final HttpServletRequest httpServletRequest;
	private final UUID modelId;
	private final UUID id;
	private final UUID parent;

	private CommonService commonService;
	private ResourceBundleService resourceBundleService;
	private static DatabaseService databaseService = new DatabaseServiceImpl();

	public CreateForm(HttpServletRequest httpServletRequest, UUID modelId, UUID id, UUID parent) {
		this.httpServletRequest = httpServletRequest;
		commonService = new CommonServiceImpl(this.httpServletRequest);
		resourceBundleService = new ResourceBundleServiceImpl(this.httpServletRequest);
		this.modelId = modelId;
		this.id = id;
		this.parent = parent;
	}

	@Override
	public ObjectFormDTO execute(Connection connection) throws Exception {
		ObjectFormDTO objectFormDTO = new ObjectFormDTO();
		ModelDTO modelDTO = ModelPreviewServiceImpl.findModel(modelId);
		commonService.checkRole(modelDTO.getPreviewRoleCode());
		LinkedHashMap<String, Object> values = new LinkedHashMap<String, Object>();

		if (StringUtils.notNull(id)) {
			values = findObjectById(id, modelDTO, connection, this.resourceBundleService);
		}

		List<ModelColumnPreviewDTO> list = new ArrayList<>();

		List<ModelColumnDTO> modelColumnDTOs = LoadStaticData.findColumnsByModelId(modelId);

		list.add(idColumn());
		if (StringUtils.notNull(this.parent)) {
			list.add(parentColumn());
		}

		for (ModelColumnDTO column : modelColumnDTOs) {
			ModelColumnPreviewDTO modelColumnPreviewDTO = new ModelColumnPreviewDTO();
			modelColumnPreviewDTO.setCode(column.getCode());
			modelColumnPreviewDTO.setColspan(column.getColspan());
			modelColumnPreviewDTO.setColumnIndex(column.getColumnIndex());
			modelColumnPreviewDTO.setColumnType(column.getColumnType());
			modelColumnPreviewDTO.setEditable(column.getEditable());
			modelColumnPreviewDTO.setShowable(column.getShowable());
			modelColumnPreviewDTO.setLength(column.getLength());
			modelColumnPreviewDTO.setName(this.resourceBundleService.getText(column.getName()));
			modelColumnPreviewDTO.setNullable(column.getNullable());
			modelColumnPreviewDTO.setRowIndex(column.getRowIndex());
			modelColumnPreviewDTO.setTextArea(column.getTextArea());
			modelColumnPreviewDTO.setValue(values.get(column.getCode()));

			if (StringUtils.notNull(this.parent) && column.getCode().equals(UpdateModel.PARENT_COLUMN_NAME)) {
				modelColumnPreviewDTO.setValue(this.parent);
			}

			if (StringUtils.notNull(column.getCodebookId())) {
				modelColumnPreviewDTO.setModelId(column.getCodebookId());
				modelColumnPreviewDTO.setListOfValues(StaticData.modelCodebook.get(column.getCodebookId()));

				List<ParentListOfValues> parentListOfValues = new ArrayList<>();
				CreateModelTable.createParentList(parentListOfValues, column.getCodebookId(), resourceBundleService,
						null, objectFormDTO);
				modelColumnPreviewDTO.setParentListOfValues(parentListOfValues);
			}

			if (StringUtils.hasText(column.getListOfValuesSql())) {
				modelColumnPreviewDTO.setListOfValues(
						createListOfValues(column.getListOfValuesSql(), connection, resourceBundleService));
			}

			if (StringUtils.isNull(id) && StringUtils.hasText(column.getDefaultValueSql())) {
				List<Object> defaultValue = databaseService.executeNativeQuery(column.getDefaultValueSql(), null,
						Object.class, connection);
				if (!defaultValue.isEmpty()) {
					modelColumnPreviewDTO.setValue(defaultValue.getFirst());
				}
			}

			if (modelColumnPreviewDTO.getColumnType().equals(ColumnType.FILE)
					&& StringUtils.notNull(modelColumnPreviewDTO.getValue())) {
				UUID fileValue = (UUID) modelColumnPreviewDTO.getValue();

				modelColumnPreviewDTO.setValue(findModelFileDtoFromId(fileValue, connection, false));

			}

			list.add(modelColumnPreviewDTO);

		}
		objectFormDTO.setFields(list);
		return objectFormDTO;
	}

	public static List<ComboboxDTO> createListOfValues(String listOfValuesQuery, Connection connection,
			ResourceBundleService resourceBundleService) {
		List<Object[]> objects = databaseService.executeNativeQuery(listOfValuesQuery, null, Object[].class,
				connection);
		return objects.stream().map(a -> new ComboboxDTO(a[0], resourceBundleService.getText(a[1].toString())))
				.toList();
	}

	public static FileUploadDTO findModelFileDtoFromId(UUID id, Connection connection, Boolean fromBase) {
		ModelFileDTO modelFileDTO = null;
		if ((StaticData.modelFiles.stream().filter(a -> a.getId().equals(id)).count() > 0) & (!fromBase)) {
			modelFileDTO = StaticData.modelFiles.stream().filter(a -> a.getId().equals(id)).findFirst().get();
		} else {
			modelFileDTO = databaseService
					.findAll(DatabaseServiceImpl.createFindByIdParameters(id), ModelFileDTO.class, connection)
					.getFirst();
		}

		return new FileUploadDTO(null, id, modelFileDTO.getFileName(), modelFileDTO.getMimeType());
	}

	private ModelColumnPreviewDTO idColumn() {
		ModelColumnPreviewDTO modelColumnPreviewDTO = new ModelColumnPreviewDTO();
		modelColumnPreviewDTO.setCode(SqlQueryWriterServiceImpl.defaultIdColumn);
		modelColumnPreviewDTO.setColumnType(ColumnType.UUID);
		modelColumnPreviewDTO.setEditable(false);
		modelColumnPreviewDTO.setName(SqlQueryWriterServiceImpl.defaultIdColumn);
		modelColumnPreviewDTO.setNullable(true);
		modelColumnPreviewDTO.setTextArea(false);
		modelColumnPreviewDTO.setValue(this.id);
		return modelColumnPreviewDTO;
	}

	private ModelColumnPreviewDTO parentColumn() {
		ModelColumnPreviewDTO modelColumnPreviewDTO = new ModelColumnPreviewDTO();
		modelColumnPreviewDTO.setCode(UpdateModel.PARENT_COLUMN_NAME);
		modelColumnPreviewDTO.setColumnType(ColumnType.UUID);
		modelColumnPreviewDTO.setEditable(false);
		modelColumnPreviewDTO.setName(UpdateModel.PARENT_COLUMN_NAME);
		modelColumnPreviewDTO.setNullable(true);
		modelColumnPreviewDTO.setTextArea(false);
		modelColumnPreviewDTO.setValue(this.parent);
		return modelColumnPreviewDTO;
	}

	public static LinkedHashMap<String, Object> findObjectById(UUID id, ModelDTO modelDTO, Connection connection,
			ResourceBundleService resourceBundleService) {
		QueryTableInfo queryTableInfo = new QueryTableInfo();
		queryTableInfo.setName(modelDTO.getCode());

		DatabaseTable<?> databaseTable = new DatabaseTable<>();
		CreateModelTable.addColumn(modelDTO, databaseTable, resourceBundleService);

		DatabaseParameter databaseParameter = DatabaseServiceImpl.createFindByIdParameters(id);

		for (DatabaseColumn column : databaseTable.getAllColumns()) {
			queryTableInfo.getColumns()
					.add(QueryColumnInfo.valueOf(column.getFieldName(), column.getFieldName(), column.getColumnType()));
		}
		List<LinkedHashMap<String, Object>> list = CreateModelTable.createListObject(queryTableInfo, databaseParameter,
				databaseTable, connection);

		if (!list.isEmpty()) {
			return list.getFirst();
		}

		throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noDataFound", modelDTO.getId() + ":" + id);
	}
}
