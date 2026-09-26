package rs.formuvia.utils;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;

import jakarta.persistence.Table;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.CheckTables;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.database.utils.TriggerInfo;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.enums.ModelType;

public class InitScriptExecute implements ExecuteQuery<Void> {

	private static DatabaseService databaseService = new DatabaseServiceImpl();
	private static CommonService commonService = new CommonServiceImpl();
	private static final String CURRENT_SCHEMA_QUERY = "select current_schema";
	public static final String ADDITIONAL_SCHEMA_TO_REPLACE = "#additional_schema#";
	public static final String CURRENT_SCHEMA_TO_REPLACE = "#current_schema#";
	public static final String TABLE_NAME_TO_REPLACE = "#table_name#";
	private static final String LISTEN_SCRIPT = "table_listen.sql";
	public static final String DATABASE_ADDITION_SCHEMA_NAME = "database.additional.shema";
	private static final String TRIGGER_LISTEN_NAME = "table_listen";

	@Override
	public Void execute(Connection connection) throws Exception {

		List<TriggerInfo> allTrigger = CheckTables.allTriggerInfos(connection);

		for (DatabaseListen databaseListen : DatabaseListen.values()) {

			String tableName = databaseListen.entityClass.getAnnotation(Table.class).name();

			if (allTrigger.stream()
					.filter(a -> a.getTableName().equals(tableName) && a.getTriggerName().equals(TRIGGER_LISTEN_NAME))
					.count() == 0) {
				createListen(connection, tableName);
			}
		}

		List<ModelDTO> listModels = databaseService.findAll(
				DatabaseParameter.valueOf(DatabaseFilter.valueOf("type", ModelType.TABLE.name())), ModelDTO.class,
				connection);
		for (ModelDTO modelDTO : listModels) {

			String tableName = modelDTO.getCode();

			if (allTrigger.stream()
					.filter(a -> a.getTableName().equals(tableName) && a.getTriggerName().equals(TRIGGER_LISTEN_NAME))
					.count() == 0) {
				createListen(connection, tableName);
			}

			if (allTrigger.stream().filter(a -> a.getTableName().equals(tableName)
					&& a.getTriggerName().equals(CheckTables.CREATE_AUDIT_TRIGGER)).count() == 0) {
				CheckTables.createAuditTrigger(connection, tableName);
			}
		}

		return null;
	}

	public static void createListen(Connection connection, String tableName) {
		String initSchemaListen = commonService.readQueryFromFile(LISTEN_SCRIPT);
		String query = new String(initSchemaListen.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
		query = query.replaceAll(ADDITIONAL_SCHEMA_TO_REPLACE, additionalSchemaName());
		query = query.replaceAll(CURRENT_SCHEMA_TO_REPLACE, findCurrentSchema(databaseService, connection));
		query = query.replaceAll(TABLE_NAME_TO_REPLACE, tableName);
		databaseService.executeUpdateQuery(query, null, connection);
	}

	public static String additionalSchemaName() {
		return StaticData.appProperties.getProperty(DATABASE_ADDITION_SCHEMA_NAME);
	}

	public static String findCurrentSchema(DatabaseService databaseService, Connection connection) {
		return databaseService.executeNativeQuery(CURRENT_SCHEMA_QUERY, null, String.class, connection).getFirst();
	}

}
