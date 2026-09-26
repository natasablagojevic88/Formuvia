package rs.formuvia.model.utils;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
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
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.database.utils.LoadStaticData;
import rs.formuvia.database.utils.QueryColumnInfo;
import rs.formuvia.database.utils.QueryDatabaseOrder;
import rs.formuvia.database.utils.QueryTableInfo;
import rs.formuvia.model.dto.ModelColumnDTO;
import rs.formuvia.model.dto.ModelDTO;
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
			this.databaseParameter.getOrders()
					.add(new QueryDatabaseOrder(SqlQueryWriterServiceImpl.defaultIdColumn, Direction.DESC));
		}

		if (StringUtils.notNull(this.parentId)) {
			this.databaseParameter.getFilters()
					.add(DatabaseFilter.valueOf(UpdateModel.PARENT_COLUMN_NAME, this.parentId.toString()));
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

		String totalQuery = sqlQueryWriterService.createTotalQuery(queryTableInfo, databaseParameter);
		Long total = databaseService.executeNativeQuery(totalQuery,
				sqlQueryWriterService.createParameters(databaseParameter.getFilters()), Long.class, connection)
				.getFirst();
		databaseTable.setTotal(total);
		databaseTable.setNumberOfPages(CreateDatabaseTable.numberOfPages(total, databaseParameter.getPageSize()));

		databaseTable.getAllColumns().removeIf(a -> a.getFieldName().equals(UpdateModel.PARENT_COLUMN_NAME));

		return databaseTable;
	}

	public static void addColumn(ModelDTO modelDTO, DatabaseTable<?> databaseTable,
			ResourceBundleService resourceBundleService) {
		List<DatabaseColumn> databaseColumns = getColumnsForModel(modelDTO, resourceBundleService);
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
			}
			listItems.add(item);
		}

		return listItems;
	}

	public static List<DatabaseColumn> getColumnsForModel(ModelDTO modelDTO,
			ResourceBundleService resourceBundleService) {
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
			databaseColumn.setFieldName(column.getCode());
			if (StringUtils.notNull(column.getCodebookId())) {
				databaseColumn.setListOfValues(
						StaticData.modelCodebook.get(column.getCodebookId()) == null ? new ArrayList<>()
								: StaticData.modelCodebook.get(column.getCodebookId()));
			}
			databaseColumn.setRequired(!column.getNullable());

			list.add(databaseColumn);
		}
		return list;
	}
}
