package rs.formuvia.database.utils;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.Table;
import lombok.RequiredArgsConstructor;
import rs.formuvia.common.entity.Track;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.utils.InitScriptExecute;

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
	private final String CREATE_AUDIT_TRIGGER = "create_audit";
	private final String AUDIT_FILE_QUERY = "audit_function.sql";

	@Override
	public Void execute(Connection connection) throws Exception {
		List<String> allTable = allTable(connection);
		List<BaseColumnInfo> allColumns = allColumns(connection);
		List<BaseIndexInfo> baseIndexInfos = allIndexes(connection);
		List<BaseConstraintInfo> baseConstraintInfos = allConstraintInfos(connection);
		List<TriggerInfo> triggerInfos = allTriggerInfos(connection);
		String queryTemplate = commonService.readQueryFromFile(AUDIT_FILE_QUERY);

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
			
			if(tableInfo.getName().equals(Track.class.getAnnotation(Table.class).name())) {
				continue;
			}

			if (triggerInfos.stream().filter(a -> a.getTableName().equals(tableInfo.getName())
					&& a.getTriggerName().equals(CREATE_AUDIT_TRIGGER)).count() == 0) {
				String query = new String(queryTemplate.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
				query = query.replaceAll(InitScriptExecute.ADDITIONAL_SCHEMA_TO_REPLACE,
						InitScriptExecute.additionalSchemaName());
				query = query.replaceAll(InitScriptExecute.CURRENT_SCHEMA_TO_REPLACE,
						InitScriptExecute.findCurrentSchema(databaseService, connection));
				query = query.replaceAll(InitScriptExecute.TABLE_NAME_TO_REPLACE,
						tableInfo.getName());
				
				databaseService.executeUpdateQuery(query, null, connection);
				

			}

		}
		return null;
	}

	public static List<String> allTable(Connection connection) {
		String query = commonService.readQueryFromFile(ALL_TABLES_QUERY_FILE);
		return databaseService.executeNativeQuery(query, null, String.class, connection);
	}

	private List<BaseColumnInfo> allColumns(Connection connection) {
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

	private List<TriggerInfo> allTriggerInfos(Connection connection) {
		String query = commonService.readQueryFromFile(ALL_TRIGGERS_QUERY_FILE);
		return databaseService.executeNativeQuery(query, null, TriggerInfo.class, connection);
	}

}
