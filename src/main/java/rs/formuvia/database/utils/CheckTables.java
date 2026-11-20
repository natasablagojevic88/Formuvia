package rs.formuvia.database.utils;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.Table;
import lombok.RequiredArgsConstructor;
import rs.formuvia.common.entity.Notification;
import rs.formuvia.common.entity.Track;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.utils.InitScriptExecute;
import rs.formuvia.utils.StringUtils;

@RequiredArgsConstructor
public class CheckTables implements ExecuteQuery<Void> {

	private static CommonService commonService = new CommonServiceImpl();
	private static DatabaseService databaseService = new DatabaseServiceImpl();
	private static final String ALL_TABLES_QUERY_FILE = "allTables.sql";
	private static final String ALL_COLUMNS_QUERY_FILE = "allColumns.sql";
	private static final String ALL_INDEX_QUERY_FILE = "allIndexes.sql";
	private static final String ALL_CONSTRAINT_QUERY_FILE = "allConstraints.sql";
	private static final String ALL_TRIGGERS_QUERY_FILE = "allTriggers.sql";
	private static final String UNIQUE_TYPE_NAME = "UNIQUE";
	private static final String FOREIGN_KEY_TYPE_NAME = "FOREIGN_KEY";
	private static final String CHECKTYPE_NAME = "CHECK";
	private final List<TableInfo> tableInfos;
	public static final String CREATE_AUDIT_TRIGGER = "create_audit";
	private static final String AUDIT_FILE_QUERY = "audit_function.sql";
	private final String CREATE_NOTIFICATION_QUERY_FILE = "notification_listen.sql";

	@Override
	public Void execute(Connection connection) throws Exception {
		List<String> allTable = allTable(connection);
		List<BaseColumnInfo> allColumns = allColumns(connection);
		List<BaseIndexInfo> baseIndexInfos = allIndexes(connection);
		List<BaseConstraintInfo> baseConstraintInfos = allConstraintInfos(connection);
		List<TriggerInfo> triggerInfos = allTriggerInfos(connection);

		for (TableInfo tableInfo : this.tableInfos) {
			if (allTable.stream().filter(a -> a.equals(tableInfo.getName())).count() == 0) {
				CreateTable createTable = new CreateTable(tableInfo);
				databaseService.executeQuery(createTable, connection);
			} else {
				List<String> baseColumnInfos = allColumns.stream()
						.filter(a -> a.getTableName().equals(tableInfo.getName())).map(a -> a.getColumnName())
						.collect(Collectors.toList());
				List<ColumnInfo> columnToInsert = tableInfo.getColumns().stream()
						.filter(a -> !baseColumnInfos.contains(a.getName())).collect(Collectors.toList());
				for (ColumnInfo columnInfo : columnToInsert) {
					databaseService.executeQuery(new CreateColumn(columnInfo), connection);
				}
			}

		}
		for (TableInfo tableInfo : this.tableInfos) {
			List<String> uniquesBase = baseConstraintInfos.stream()
					.filter(a -> tableInfo.getName().equals(a.getTable()))
					.filter(a -> UNIQUE_TYPE_NAME.equals(a.getType())).map(a -> a.getName())
					.collect(Collectors.toList());
			List<UniqueConstraintInfo> uniqueConstraintInfos = tableInfo.getUniqueContraints().stream()
					.filter(a -> !uniquesBase.contains(a.getName())).collect(Collectors.toList());
			for (UniqueConstraintInfo uniqueConstraintInfo : uniqueConstraintInfos) {
				databaseService.executeQuery(new CreateUniqueConstraint(uniqueConstraintInfo), connection);
			}

			List<String> foreignKeyBase = baseConstraintInfos.stream()
					.filter(a -> tableInfo.getName().equals(a.getTable()))
					.filter(a -> FOREIGN_KEY_TYPE_NAME.equals(a.getType())).map(a -> a.getName())
					.collect(Collectors.toList());
			List<ForeignKeyInfo> foreignKeyInfos = tableInfo.getForeignKeys().stream()
					.filter(a -> !foreignKeyBase.contains(a.getName())).collect(Collectors.toList());
			for (ForeignKeyInfo foreignKeyInfo : foreignKeyInfos) {
				databaseService.executeQuery(new CreateForeignKey(foreignKeyInfo), connection);
			}

			List<String> baseIndexInfoBase = baseIndexInfos.stream()
					.filter(a -> tableInfo.getName().equals(a.getTableName())).map(a -> a.getIndexName())
					.collect(Collectors.toList());
			List<IndexInfo> indexInfos = tableInfo.getIndexes().stream()
					.filter(a -> !baseIndexInfoBase.contains(a.getName())).collect(Collectors.toList());
			for (IndexInfo indexInfo : indexInfos) {
				databaseService.executeQuery(new CreateIndex(indexInfo), connection);
			}

			List<ColumnInfo> columnInfosWithListOfValue = tableInfo.getColumns().stream()
					.filter(a -> !a.getListOfValues().isEmpty()).collect(Collectors.toList());

			for (ColumnInfo columnInfo : columnInfosWithListOfValue) {
				String checkConstraintText = CreateTable.createListOfValuesConstraint(columnInfo.getName(),
						columnInfo.getListOfValues());
				String checkConstraintName = CreateTable.createCheckConstraintEnumName(columnInfo);
				String currentCheckConstraintText = baseConstraintInfos.stream()
						.filter(a -> tableInfo.getName().equals(a.getTable()))
						.filter(a -> CHECKTYPE_NAME.equals(a.getType()))
						.filter(a -> checkConstraintName.equals(a.getName())).map(a -> a.getDefinition()).findFirst()
						.orElse(null);
				if (currentCheckConstraintText == null) {
					databaseService.executeQuery(new CreateEnumCheckConstraint(columnInfo), connection);
				} else {
					if (!checkConstraintText.equals(currentCheckConstraintText)) {
						databaseService.executeQuery(new CreateEnumCheckConstraint(columnInfo), connection);
					}
				}

			}

			if (tableInfo.getName().equals(Track.class.getAnnotation(Table.class).name())) {
				continue;
			}

			if (tableInfo.getName().equals(Notification.class.getAnnotation(Table.class).name())) {
				if (triggerInfos.stream().filter(a -> a.getTableName().equals(tableInfo.getName())
						&& a.getTriggerName().equals(InitScriptExecute.TRIGGER_LISTEN_NAME)).count() == 0) {

					String query = commonService.readQueryFromFile(CREATE_NOTIFICATION_QUERY_FILE);
					query = createSqlQueryWithReplace(query, connection, null);
					databaseService.executeUpdateQuery(query, null, connection);

				}
			}

			if (triggerInfos.stream().filter(a -> a.getTableName().equals(tableInfo.getName())
					&& a.getTriggerName().equals(CREATE_AUDIT_TRIGGER)).count() == 0) {

				createAuditTrigger(connection, tableInfo.getName());
			}

		}
		return null;
	}

	public static void createAuditTrigger(Connection connection, String tableName) {
		String queryTemplate = commonService.readQueryFromFile(AUDIT_FILE_QUERY);

		databaseService.executeUpdateQuery(createSqlQueryWithReplace(queryTemplate, connection, tableName), null,
				connection);
	}

	private static String createSqlQueryWithReplace(String queryIn, Connection connection, String tableName) {
		String query = new String(queryIn.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
		query = query.replaceAll(InitScriptExecute.ADDITIONAL_SCHEMA_TO_REPLACE,
				InitScriptExecute.additionalSchemaName());
		query = query.replaceAll(InitScriptExecute.CURRENT_SCHEMA_TO_REPLACE,
				InitScriptExecute.findCurrentSchema(databaseService, connection));

		if (StringUtils.hasText(tableName))
			query = query.replaceAll(InitScriptExecute.TABLE_NAME_TO_REPLACE, tableName);

		return query;
	}

	public static List<String> allTable(Connection connection) {
		String query = commonService.readQueryFromFile(ALL_TABLES_QUERY_FILE);
		return databaseService.executeNativeQuery(query, null, String.class, connection);
	}

	public static List<BaseColumnInfo> allColumns(Connection connection) {
		String query = commonService.readQueryFromFile(ALL_COLUMNS_QUERY_FILE);
		return databaseService.executeNativeQuery(query, null, BaseColumnInfo.class, connection);
	}

	private List<BaseIndexInfo> allIndexes(Connection connection) {
		String query = commonService.readQueryFromFile(ALL_INDEX_QUERY_FILE);
		return databaseService.executeNativeQuery(query, null, BaseIndexInfo.class, connection);
	}

	private List<BaseConstraintInfo> allConstraintInfos(Connection connection) {
		String query = commonService.readQueryFromFile(ALL_CONSTRAINT_QUERY_FILE);
		return databaseService.executeNativeQuery(query, null, BaseConstraintInfo.class, connection);
	}

	public static List<TriggerInfo> allTriggerInfos(Connection connection) {
		String query = commonService.readQueryFromFile(ALL_TRIGGERS_QUERY_FILE);
		return databaseService.executeNativeQuery(query, null, TriggerInfo.class, connection);
	}

}
