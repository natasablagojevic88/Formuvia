package rs.formuvia.model.utils;

import java.net.HttpURLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modelmapper.ModelMapper;

import lombok.RequiredArgsConstructor;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.Select;
import rs.formuvia.common.dto.ComboboxDTO;
import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.database.enums.Direction;
import rs.formuvia.database.enums.SearchOperation;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.SqlQueryWriterService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.service.impl.SqlQueryWriterServiceImpl;
import rs.formuvia.database.utils.BaseColumnInfo;
import rs.formuvia.database.utils.CheckTables;
import rs.formuvia.database.utils.ColumnInfo;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.database.utils.ForeignKeyInfo;
import rs.formuvia.database.utils.QueryColumnInfo;
import rs.formuvia.database.utils.QueryDatabaseOrder;
import rs.formuvia.database.utils.QueryTableInfo;
import rs.formuvia.database.utils.TableInfo;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.exceptions.NotNullException;
import rs.formuvia.exceptions.UniqueException;
import rs.formuvia.model.dto.ModelColumnDTO;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.entity.Model;
import rs.formuvia.model.entity.ModelColumn;
import rs.formuvia.model.service.impl.ModelPreviewServiceImpl;
import rs.formuvia.quartz.jobs.DatabaseListenCheckConnectionJob;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

@RequiredArgsConstructor
public class UpdateColumnModel implements ExecuteQuery<ModelColumnDTO> {

	private final ModelColumnDTO modelColumnDTO;

	private final Integer LENGHT = 255;
	public static String LISTEN_PREFIX = "listen_";

	private static DatabaseService databaseService = new DatabaseServiceImpl();
	private ModelMapper modelMapper = new ModelMapper();
	private static final Pattern COLUMN_NAME = Pattern.compile("^[a-z][a-z0-9_]{0,62}$");
	private static SqlQueryWriterService sqlQueryWriterService = new SqlQueryWriterServiceImpl();

	@Override
	public ModelColumnDTO execute(Connection connection) throws Exception {
		Model model = databaseService.findById(modelColumnDTO.getModelId(), Model.class, connection);
		ModelColumn modelColumn = modelColumnDTO.getId() == null ? new ModelColumn()
				: databaseService.findById(modelColumnDTO.getId(), ModelColumn.class, connection);
		if (modelColumnDTO.getId() != null) {
			if (!modelColumnDTO.getColumnType().equals(modelColumn.getColumnType())) {
				modelColumnDTO.setColumnType(modelColumn.getColumnType());
			}

			if (!modelColumnDTO.getNullable().equals(modelColumn.getNullable())) {
				modelColumnDTO.setNullable(modelColumn.getNullable());
			}

			if (modelColumnDTO.getColumnType().equals(ColumnType.UUID)) {
				if (!modelColumnDTO.getCodebookId().equals(modelColumn.getCodebook().getId())) {
					modelColumnDTO.setCodebookId(modelColumn.getCodebook().getId());
				}
			}
		}
		List<ModelColumnDTO> list = databaseService.findAll(
				DatabaseParameter.valueOf(DatabaseFilter.valueOf("modelId", modelColumnDTO.getModelId().toString())),
				ModelColumnDTO.class, connection);
		checkColumnModel(model, modelColumnDTO, connection, list);

		UUID loadTable = checkTableCodebook(modelColumn, modelColumnDTO, connection);

		modelMapper.map(modelColumnDTO, modelColumn);
		modelColumn = databaseService.save(modelColumn, connection);

		if (StringUtils.isNull(modelColumnDTO.getId())) {
			createColumn(connection, modelColumn, model.getCode());
		}

		if (loadTable != null) {
			final ModelDTO codebookModel = databaseService.findById(loadTable, ModelDTO.class, connection);
			final List<ModelColumnDTO> listColumn = findColumnList(loadTable, connection);
			connection.commit();
			new Thread(() -> {
				loadModelStaticList(codebookModel, listColumn, null);
			}).start();
		}

		return modelMapper.map(modelColumn, ModelColumnDTO.class);
	}

	public static List<ModelColumnDTO> findColumnList(UUID modelId, Connection connection) {
		DatabaseParameter columnDatabaseParameter = new DatabaseParameter();
		columnDatabaseParameter.getFilters().add(DatabaseFilter.valueOf("modelId", modelId.toString()));
		columnDatabaseParameter.getOrders().add(QueryDatabaseOrder.valueOf("rowIndex", Direction.ASC));
		columnDatabaseParameter.getOrders().add(QueryDatabaseOrder.valueOf("columnIndex", Direction.ASC));
		List<ModelColumnDTO> columnDTOs = databaseService.findAll(columnDatabaseParameter, ModelColumnDTO.class,
				connection);
		List<ModelColumnDTO> columnsWithDesc = columnDTOs.stream().filter(a -> a.getInDescriptionForCodebook())
				.collect(Collectors.toList());

		return columnsWithDesc;
	}

	private void createColumn(Connection connection, ModelColumn modelColumn, String tableName) {
		List<BaseColumnInfo> allColumns = CheckTables.allColumns(connection);

		if (allColumns.stream()
				.filter(a -> a.getTableName().equals(tableName) && a.getColumnName().equals(modelColumnDTO.getCode()))
				.count() > 0) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "columnAlreadyExists",
					modelColumnDTO.getCode());
		}

		TableInfo tableInfo = new TableInfo();
		tableInfo.setName(tableName);

		ColumnInfo columnInfo = new ColumnInfo();
		columnInfo.setName(modelColumnDTO.getCode());
		columnInfo.setColumnDefinition(modelColumnDTO.getTextArea() ? "text" : null);
		columnInfo.setColumnType(modelColumnDTO.getColumnType());
		columnInfo.setIsPrimary(false);
		columnInfo.setLength(LENGHT);
		columnInfo.setNullable(modelColumnDTO.getNullable());
		columnInfo.setScale(modelColumnDTO.getLength());
		columnInfo.setTableName(tableName);
		tableInfo.getColumns().add(columnInfo);

		if (modelColumnDTO.getColumnType().equals(ColumnType.UUID)) {
			ForeignKeyInfo foreignKeyInfo = new ForeignKeyInfo();
			foreignKeyInfo.setCascadeDelete(false);
			foreignKeyInfo.setColumnName(columnInfo.getName());
			foreignKeyInfo.setName("fk_" + tableName + "_" + columnInfo.getName());
			foreignKeyInfo.setTableName(tableName);
			foreignKeyInfo.setReferenceTable(modelColumn.getCodebook().getCode());
			tableInfo.getForeignKeys().add(foreignKeyInfo);
		}

		CheckTables createTable = new CheckTables(Arrays.asList(tableInfo));
		databaseService.executeQuery(createTable, connection);

	}

	private UUID checkTableCodebook(ModelColumn modelColumn, ModelColumnDTO modelDTO, Connection connection) {
		if (modelDTO.getColumnType().equals(ColumnType.UUID) && StringUtils.isNull(modelDTO.getId())) {
			DatabaseParameter databaseParameter = DatabaseParameter.valueOf(
					new DatabaseFilter[] { DatabaseFilter.valueOf("codebook", modelDTO.getCodebookId().toString()) });

			if (!databaseService.exists(databaseParameter, ModelColumn.class, connection)) {
				return modelDTO.getCodebookId();
			}
		}

		if ((modelDTO.getId() == null && modelDTO.getInDescriptionForCodebook()) || (modelDTO.getId() != null
				&& (!modelDTO.getInDescriptionForCodebook().equals(modelColumn.getInDescriptionForCodebook())))) {
			DatabaseParameter databaseParameter = DatabaseParameter.valueOf(
					new DatabaseFilter[] { DatabaseFilter.valueOf("codebook", modelDTO.getModelId().toString()) });

			if (databaseService.exists(databaseParameter, ModelColumn.class, connection)) {
				return modelDTO.getModelId();
			}
		}

		return null;
	}

	public static void loadModelStaticList(ModelDTO model, List<ModelColumnDTO> columnsWithDesc,
			Connection connection) {

		QueryTableInfo queryTableInfo = new QueryTableInfo();
		queryTableInfo.setName(model.getCode());
		queryTableInfo.getColumns().add(QueryColumnInfo.valueOf("id", "id", ColumnType.UUID));

		for (ModelColumnDTO modelColumnDTO : columnsWithDesc) {
			queryTableInfo.getColumns().add(QueryColumnInfo.valueOf(modelColumnDTO.getCode(), modelColumnDTO.getCode(),
					modelColumnDTO.getColumnType()));
		}
		String query = sqlQueryWriterService.createSelectQuery(queryTableInfo, new DatabaseParameter());

		List<Object[]> objects = connection == null ? databaseService.executeNativeQuery(query, null, Object[].class)
				: databaseService.executeNativeQuery(query, null, Object[].class, connection);
		List<ComboboxDTO> values = new ArrayList<>();
		for (Object[] item : objects) {
			UUID value = UUID.fromString(item[0].toString());
			String option = item.length == 1 ? value.toString() : Arrays.asList(item).stream().skip(1).map(a -> {
				if (StringUtils.isNull(a))
					return "";
				else
					return " " + a.toString();
			}).collect(Collectors.joining()).trim();
			values.add(new ComboboxDTO(value, option));
		}
		if (!StaticData.modelsToListen.contains(model.getId())) {
			initListen(model.getId());
		}
		StaticData.modelCodebook.put(model.getId(), values);

	}

	public static void initListen(UUID modelId) {
		if (StaticData.databaseListenConnection == null) {
			return;
		}
		Logger logger = LogManager.getLogger(UpdateColumnModel.class);
		ModelDTO modelDTO = ModelPreviewServiceImpl.findModel(modelId);

		try {
			PreparedStatement preparedStatement = StaticData.databaseListenConnection.prepareStatement(
					DatabaseListenCheckConnectionJob.LISTEN_QUERY + LISTEN_PREFIX + modelDTO.getCode());
			preparedStatement.execute();
			preparedStatement.close();
			StaticData.modelsToListen.add(modelId);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}

	private void checkColumnModel(Model model, ModelColumnDTO modelColumnDTO, Connection connection,
			List<ModelColumnDTO> list) {
		if (modelColumnDTO.getColumnIndex() + modelColumnDTO.getColspan() - 1 > model.getColumnNumber()) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "columnAndColspanNowAllow", null);
		}

		if (modelColumnDTO.getRowIndex() > model.getRowNumber()) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "rowIndexNotAllowed", null);
		}

		List<ModelColumnDTO> listInRow = list.stream().filter(a -> a.getRowIndex().equals(modelColumnDTO.getRowIndex()))
				.filter(a -> {
					if (modelColumnDTO.getId() == null)
						return true;
					else
						return !a.getId().equals(modelColumnDTO.getId());
				}).collect(Collectors.toList());

		Set<Integer> usedIndex = new HashSet<>();
		for (ModelColumnDTO columnDTO : listInRow) {
			for (int i = columnDTO.getColumnIndex(); i <= columnDTO.getColumnIndex() + columnDTO.getColspan()
					- 1; i++) {
				usedIndex.add(i);
			}
		}

		for (int i = modelColumnDTO.getColumnIndex(); i <= modelColumnDTO.getColumnIndex() + modelColumnDTO.getColspan()
				- 1; i++) {
			if (usedIndex.contains(i)) {
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "columnIsInUsed", null);
			}
		}

		if (!COLUMN_NAME.matcher(modelColumnDTO.getCode()).matches()) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "invalidColumnName",
					modelColumnDTO.getCode());
		}

		DatabaseParameter databaseParameter = DatabaseParameter
				.valueOf(new DatabaseFilter[] { DatabaseFilter.valueOf("code", modelColumnDTO.getCode()),
						DatabaseFilter.valueOf("modelId", modelColumnDTO.getModelId().toString()) });
		if (modelColumnDTO.getId() != null)
			databaseParameter = DatabaseParameter.valueOf(new DatabaseFilter[] {
					DatabaseFilter.valueOf("code", modelColumnDTO.getCode()),
					DatabaseFilter.valueOf("modelId", modelColumnDTO.getModelId().toString()),
					DatabaseFilter.valueOf("id", SearchOperation.NOT_EQUALS, modelColumnDTO.getId().toString()) });

		if (databaseService.exists(databaseParameter, ModelColumnDTO.class, connection))
			throw new UniqueException(UniqueException.findFieldFromList(ModelColumnDTO.class, "code"),
					modelColumnDTO.getCode());

		databaseParameter = DatabaseParameter
				.valueOf(new DatabaseFilter[] { DatabaseFilter.valueOf("name", modelColumnDTO.getName()),
						DatabaseFilter.valueOf("modelId", modelColumnDTO.getModelId().toString()) });
		if (modelColumnDTO.getId() != null)
			databaseParameter = DatabaseParameter.valueOf(new DatabaseFilter[] {
					DatabaseFilter.valueOf("name", modelColumnDTO.getName()),
					DatabaseFilter.valueOf("modelId", modelColumnDTO.getModelId().toString()),
					DatabaseFilter.valueOf("id", SearchOperation.NOT_EQUALS, modelColumnDTO.getId().toString()) });

		if (databaseService.exists(databaseParameter, ModelColumnDTO.class, connection))
			throw new UniqueException(UniqueException.findFieldFromList(ModelColumnDTO.class, "name"),
					modelColumnDTO.getName());

		if (modelColumnDTO.getColumnType().equals(ColumnType.UUID)
				&& StringUtils.isNull(modelColumnDTO.getCodebookId()))
			throw new NotNullException(UniqueException.findFieldFromList(ModelColumnDTO.class, "codebookId"));

		if (!modelColumnDTO.getColumnType().equals(ColumnType.BIGDECIMAL)) {
			modelColumnDTO.setLength(255);
		} else {

			if (StringUtils.isNull(modelColumnDTO.getLength())) {
				throw new NotNullException(UniqueException.findFieldFromList(ModelColumnDTO.class, "length"));
			}
		}

		if (!modelColumnDTO.getColumnType().equals(ColumnType.STRING)) {
			modelColumnDTO.setTextArea(false);
		}

		if (modelColumnDTO.getColumnType().equals(ColumnType.UUID)) {
			modelColumnDTO.setInDescriptionForCodebook(false);
		}

		if (StringUtils.hasText(modelColumnDTO.getDefaultValueSql())) {
			Statement statement;
			try {
				statement = CCJSqlParserUtil.parse(modelColumnDTO.getDefaultValueSql());
				if (!(statement instanceof Select)) {
					throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "onlySelectAllowed",
							modelColumnDTO.getDefaultValueSql());
				} else {

					Select selectQuery = (Select) statement;
					boolean hasStar = selectQuery.getPlainSelect().getSelectItems().stream()
							.anyMatch(a -> a.getExpression() instanceof AllColumns);

					if (hasStar) {
						throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "selectStarNotAllowed",
								modelColumnDTO.getDefaultValueSql());
					}

					if (selectQuery.getPlainSelect().getSelectItems().size() != 1) {
						throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "selectMustHave1Column",
								modelColumnDTO.getDefaultValueSql());
					}

				}
			} catch (JSQLParserException e) {
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "invalidQuery",
						modelColumnDTO.getDefaultValueSql());
			}

		}

		if (StringUtils.hasText(modelColumnDTO.getListOfValuesSql())) {
			Statement statement;
			try {
				statement = CCJSqlParserUtil.parse(modelColumnDTO.getListOfValuesSql());
				if (!(statement instanceof Select)) {
					throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "onlySelectAllowed",
							modelColumnDTO.getListOfValuesSql());
				} else {
					Select selectQuery = (Select) statement;
					boolean hasStar = selectQuery.getPlainSelect().getSelectItems().stream()
							.anyMatch(a -> a.getExpression() instanceof AllColumns);

					if (hasStar) {
						throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "selectStarNotAllowed",
								modelColumnDTO.getListOfValuesSql());
					}

					if (selectQuery.getPlainSelect().getSelectItems().size() != 2) {
						throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "selectMustHave2Column",
								modelColumnDTO.getListOfValuesSql());
					}

				}
			} catch (JSQLParserException e) {
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "invalidQuery",
						modelColumnDTO.getListOfValuesSql());
			}

		}

	}

}
