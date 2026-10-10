package rs.formuvia.model.utils;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import rs.formuvia.common.dto.ComboboxDTO;
import rs.formuvia.common.dto.FileUploadDTO;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.ResourceBundleService;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.common.service.impl.ResourceBundleServiceImpl;
import rs.formuvia.common.service.impl.SessionServiceImpl;
import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.database.enums.Direction;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.SqlQueryWriterService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.service.impl.SqlQueryWriterServiceImpl;
import rs.formuvia.database.utils.CreateDatabaseTable;
import rs.formuvia.database.utils.DatabaseColumn;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseSubTable;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.database.utils.LoadStaticData;
import rs.formuvia.database.utils.ParentListOfValues;
import rs.formuvia.database.utils.QueryColumnInfo;
import rs.formuvia.database.utils.QueryDatabaseOrder;
import rs.formuvia.database.utils.QueryTableInfo;
import rs.formuvia.model.dto.ModelColumnDTO;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.dto.ObjectFormDTO;
import rs.formuvia.model.enums.ModelType;
import rs.formuvia.model.service.impl.ModelPreviewServiceImpl;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

public class CreateModelTable implements ExecuteQuery<DatabaseTable<LinkedHashMap<String, Object>>> {

	private final DatabaseParameter databaseParameter;
	private final HttpServletRequest httpServletRequest;
	private final UUID modelId;
	private final UUID parentId;
	private static final String idColumnName = "common.id";

	private CommonService commonService;
	private ResourceBundleService resourceBundleService;
	private static SqlQueryWriterService sqlQueryWriterService = new SqlQueryWriterServiceImpl();
	private static DatabaseService databaseService = new DatabaseServiceImpl();

	public CreateModelTable(DatabaseParameter databaseParameter, HttpServletRequest httpServletRequest, UUID modelId,
			UUID parentId) {
		this.databaseParameter = databaseParameter;
		this.httpServletRequest = httpServletRequest;
		this.commonService = new CommonServiceImpl(this.httpServletRequest);
		this.resourceBundleService = new ResourceBundleServiceImpl(this.httpServletRequest);
		this.modelId = modelId;
		this.parentId = parentId;
	}

	@Override
	public DatabaseTable<LinkedHashMap<String, Object>> execute(Connection connection) throws Exception {
		ModelDTO modelDTO = ModelPreviewServiceImpl.findModel(modelId);
		commonService.checkRole(modelDTO.getPreviewRoleCode());
		if (this.databaseParameter.getOrders().isEmpty()) {
			List<ModelColumnDTO> initSortColumns = StaticData.modelColumns.stream()
					.filter(a -> a.getModelId().equals(modelId)).filter(a -> StringUtils.notNull(a.getInitSortOrder()))
					.sorted(Comparator.comparing(ModelColumnDTO::getInitSortOrder)).collect(Collectors.toList());
			for (ModelColumnDTO modelColumnDTO : initSortColumns) {
				this.databaseParameter.getOrders().add(
						QueryDatabaseOrder.valueOf(modelColumnDTO.getCode(), modelColumnDTO.getInitSortDirection()));
			}

		}
		this.databaseParameter.getOrders()
				.add(new QueryDatabaseOrder(SqlQueryWriterServiceImpl.defaultIdColumn, Direction.DESC));

		if (StringUtils.notNull(this.parentId)) {
			this.databaseParameter.getFilters().add(
					DatabaseFilter.valueOf(UpdateModel.PARENT_COLUMN_NAME, this.parentId.toString(), ColumnType.UUID));
		}

		DatabaseTable<LinkedHashMap<String, Object>> databaseTable = new DatabaseTable<>();
		databaseTable.setName(resourceBundleService.getText(modelDTO.getName()));
		databaseTable.setDescription(StringUtils.hasText(modelDTO.getDescription())
				? resourceBundleService.getText(modelDTO.getDescription())
				: null);
		databaseTable.setSaveUrl(ApiRoute.modelPreviewUpdate.replace(SessionServiceImpl.MODEL_NAME_TO_REPLACE,
				modelDTO.getId().toString()));

		addColumn(modelDTO, databaseTable, this.resourceBundleService);

		QueryTableInfo queryTableInfo = new QueryTableInfo();
		queryTableInfo.setName(modelDTO.getCode());

		for (DatabaseColumn column : databaseTable.getAllColumns()) {
			queryTableInfo.getColumns()
					.add(QueryColumnInfo.valueOf(column.getFieldName(), column.getFieldName(), column.getColumnType()));
		}

		databaseTable.setList(createListObject(queryTableInfo, databaseParameter, databaseTable, connection));
		List<DatabaseColumn> totalColumns = databaseTable.getColumn().stream()
				.filter(a -> a.getColumnType().equals(ColumnType.BIGDECIMAL)).collect(Collectors.toList());

		String totalQuery = sqlQueryWriterService.createTotalQuery(queryTableInfo, databaseParameter,
				totalColumns.stream().map(a -> SqlQueryWriterServiceImpl.mainTableAlias + "." + a.getFieldName())
						.toArray(String[]::new));
		Object[] totalResult = databaseService.executeNativeQuery(totalQuery,
				sqlQueryWriterService.createParameters(databaseParameter.getFilters()), Object[].class, connection)
				.getFirst();

		Long total = ((Number) totalResult[0]).longValue();

		int totalIndexCount = 0;
		for (DatabaseColumn totalColumn : totalColumns) {
			totalIndexCount++;
			databaseTable.getTotalColumns().put(totalColumn.getFieldName(),
					new BigDecimal(totalResult[totalIndexCount].toString()));
		}

		databaseTable.setTotal(total);
		databaseTable.setNumberOfPages(CreateDatabaseTable.numberOfPages(total, databaseParameter.getPageSize()));

		databaseTable.getAllColumns().removeIf(a -> a.getFieldName().equals(UpdateModel.PARENT_COLUMN_NAME));
		databaseTable.setSubTables(findSubTable(modelDTO, resourceBundleService));
		databaseTable.setHasAdd(commonService.hasRole(modelDTO.getAddRoleCode()));
		databaseTable.setHasUpdate(commonService.hasRole(modelDTO.getUpdateRoleCode()));
		databaseTable.setHasDelete(commonService.hasRole(modelDTO.getDeleteRoleCode()));
		return databaseTable;
	}

	private List<DatabaseSubTable> findSubTable(ModelDTO modelDTO, ResourceBundleService resourceBundleService) {
		return StaticData.models.stream().filter(a -> StringUtils.notNull(a.getParentId()))
				.filter(a -> a.getParentId().equals(modelDTO.getId()))
				.filter(a -> commonService.hasRole(a.getPreviewRoleCode()))
				.map(a -> new DatabaseSubTable(this.resourceBundleService.getText(a.getName()), a.getId(), a.getIcon()))
				.sorted(Comparator.comparing(DatabaseSubTable::getName)).collect(Collectors.toList());

	}

	public static void addColumn(ModelDTO modelDTO, DatabaseTable<?> databaseTable,
			ResourceBundleService resourceBundleService) {
		List<DatabaseColumn> databaseColumns = getColumnsForModel(modelDTO, resourceBundleService, databaseTable);
		for (DatabaseColumn databaseColumn : databaseColumns) {
			databaseTable.getAllColumns().add(databaseColumn);

			if (StaticData.modelColumns.stream().filter(a -> a.getModelId().equals(modelDTO.getId()))
					.filter(a -> a.getCode().equals(databaseColumn.getFieldName())).filter(a -> a.getShowInTable())
					.count() > 0) {
				databaseTable.getColumn().add(databaseColumn);
			}

		}
	}

	public static List<LinkedHashMap<String, Object>> createListObject(QueryTableInfo queryTableInfo,
			DatabaseParameter databaseParameter, DatabaseTable<?> databaseTable, Connection connection) {
		List<LinkedHashMap<String, Object>> listItems = new ArrayList<>();
		Map<Integer, Object> parameters = sqlQueryWriterService.createParameters(databaseParameter.getFilters());
		List<Object[]> list = databaseService.executeNativeQuery(
				sqlQueryWriterService.createSelectQuery(queryTableInfo, databaseParameter), parameters, Object[].class,
				connection);

		for (Object[] objects : list) {
			int index = -1;
			LinkedHashMap<String, Object> item = new LinkedHashMap<>();
			for (DatabaseColumn column : databaseTable.getAllColumns()) {
				index++;
				item.put(column.getFieldName(), objects[index]);

				if (column.getColumnType().equals(ColumnType.FILE) && StringUtils.notNull(objects[index])) {
					UUID modelFileId = UUID.fromString(objects[index].toString());
					FileUploadDTO fileUploadDTO = CreateForm.findModelFileDtoFromId(modelFileId, connection, true);
					column.getListOfValues().add(new ComboboxDTO(fileUploadDTO.getId(), fileUploadDTO.getFileName()));
				}
			}
			listItems.add(item);
		}

		return listItems;
	}

	public static List<DatabaseColumn> getColumnsForModel(ModelDTO modelDTO,
			ResourceBundleService resourceBundleService, DatabaseTable<?> databaseTable) {
		List<ModelColumnDTO> columns = LoadStaticData.findColumnsByModelId(modelDTO.getId());
		List<DatabaseColumn> list = new ArrayList<>();
		DatabaseColumn idDatabaseColumn = new DatabaseColumn();
		idDatabaseColumn.setColumnType(ColumnType.UUID);
		idDatabaseColumn.setDescription(resourceBundleService.getText(idColumnName));
		idDatabaseColumn.setEditable(false);
		idDatabaseColumn.setFieldName(SqlQueryWriterServiceImpl.defaultIdColumn);
		idDatabaseColumn.setRequired(false);
		list.add(idDatabaseColumn);

		if (UpdateObject.tableHasParent(modelDTO)) {
			DatabaseColumn parentDatabaseColumn = new DatabaseColumn();
			parentDatabaseColumn.setColumnType(ColumnType.UUID);
			parentDatabaseColumn.setDescription(UpdateModel.PARENT_COLUMN_NAME);
			parentDatabaseColumn.setEditable(false);
			parentDatabaseColumn.setFieldName(UpdateModel.PARENT_COLUMN_NAME);
			parentDatabaseColumn.setRequired(false);
			list.add(parentDatabaseColumn);
		}

		for (ModelColumnDTO column : columns) {
			DatabaseColumn databaseColumn = new DatabaseColumn();
			databaseColumn.setColumnType(column.getColumnType());
			databaseColumn.setDescription(resourceBundleService.getText(column.getName()));
			databaseColumn.setEditable(column.getEditable());
			databaseColumn.setLength(column.getLength());
			databaseColumn.setFieldName(column.getCode());
			databaseColumn.setInDescription(column.getInDescriptionForCodebook());
			if (StringUtils.notNull(column.getCodebookId())) {
				databaseColumn.setModelId(column.getCodebookId());
				databaseColumn.setListOfValues(
						StaticData.modelCodebook.get(column.getCodebookId()) == null ? new ArrayList<>()
								: StaticData.modelCodebook.get(column.getCodebookId()));
				List<ParentListOfValues> parentListOfValues = new ArrayList<>();
				createParentList(parentListOfValues, column.getCodebookId(), resourceBundleService, databaseTable,
						null);
				databaseColumn.setParentList(parentListOfValues);
			}
			databaseColumn.setRequired(!column.getNullable());

			if (StaticData.modelColumnsConditions.stream().filter(a -> a.getModelColumnId().equals(column.getId()))
					.count() > 0) {
				databaseColumn.setConditions(StaticData.modelColumnsConditions.stream()
						.filter(a -> a.getModelColumnId().equals(column.getId())).collect(Collectors.toList()));
			}

			list.add(databaseColumn);
		}
		return list;
	}

	public static void createParentList(List<ParentListOfValues> parentListOfValues, UUID modelId,
			ResourceBundleService resourceBundleService, DatabaseTable<?> databaseTable, ObjectFormDTO objectFormDTO) {

		ModelDTO parent = findParentTable(modelId);

		if (StringUtils.isNull(parent)) {
			return;
		}

		ParentListOfValues parentListOfValue = new ParentListOfValues();
		parentListOfValue.setChild(modelId);
		parentListOfValue.setParent(parent.getId());
		parentListOfValue.setName(resourceBundleService.getText(parent.getName()));
		parentListOfValues.add(parentListOfValue);
		if (databaseTable != null) {
			if (databaseTable.getParentCodebook().get(parent.getId()) == null) {
				databaseTable.getParentCodebook().put(parent.getId(),
						StaticData.modelCodebook.get(parent.getId()) == null ? new ArrayList<>()
								: StaticData.modelCodebook.get(parent.getId()));
			}
		}

		if (objectFormDTO != null) {
			if (objectFormDTO.getParentCodebook().get(parent.getId()) == null) {
				objectFormDTO.getParentCodebook().put(parent.getId(),
						StaticData.modelCodebook.get(parent.getId()) == null ? new ArrayList<>()
								: StaticData.modelCodebook.get(parent.getId()));
			}
		}

		createParentList(parentListOfValues, parent.getId(), resourceBundleService, databaseTable, objectFormDTO);

	}

	public static ModelDTO findParentTable(UUID modelId) {
		ModelDTO modelDTO = ModelPreviewServiceImpl.findModel(modelId);
		if (StringUtils.isNull(modelDTO.getParentId())) {
			return null;
		}

		ModelDTO parent = ModelPreviewServiceImpl.findModel(modelDTO.getParentId());

		if (parent.getType().equals(ModelType.MENU)) {
			return null;
		}

		return parent;
	}
}
