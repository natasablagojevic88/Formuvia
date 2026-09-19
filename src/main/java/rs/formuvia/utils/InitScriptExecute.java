package rs.formuvia.utils;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;

import jakarta.persistence.Table;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.ExecuteQuery;

public class InitScriptExecute implements ExecuteQuery<Void> {

	private DatabaseService databaseService = new DatabaseServiceImpl();
	private CommonService commonService = new CommonServiceImpl();
	private static final String CURRENT_SCHEMA_QUERY = "select current_schema";
	public static final String ADDITIONAL_SCHEMA_TO_REPLACE = "#additional_schema#";
	private final String TABLE_NAME_LISTEN_NAME_TO_REPLACE = "#table_name_listen_name#";
	public static final String CURRENT_SCHEMA_TO_REPLACE = "#current_schema#";
	public static final String TABLE_NAME_TO_REPLACE = "#table_name#";
	private final String LISTEN_SCRIPT = "table_listen.sql";
	public static final String DATABASE_ADDITION_SCHEMA_NAME = "database.additional.shema";

	@Override
	public Void execute(Connection connection) throws Exception {

		String currentSchema = findCurrentSchema(databaseService, connection);
		String additionalSchemaName = additionalSchemaName();
		String initSchemaListen = commonService.readQueryFromFile(LISTEN_SCRIPT);

		for (DatabaseListen databaseListen : DatabaseListen.values()) {
			String query = new String(initSchemaListen.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
			query = query.replaceAll(ADDITIONAL_SCHEMA_TO_REPLACE, additionalSchemaName);
			query = query.replaceAll(TABLE_NAME_LISTEN_NAME_TO_REPLACE, databaseListen.name());
			query = query.replaceAll(CURRENT_SCHEMA_TO_REPLACE, currentSchema);
			query = query.replaceAll(TABLE_NAME_TO_REPLACE,
					databaseListen.entityClass.getAnnotation(Table.class).name());
			databaseService.executeUpdateQuery(query, null, connection);
		}

		return null;
	}

	public static String additionalSchemaName() {
		return StaticData.appProperties.getProperty(DATABASE_ADDITION_SCHEMA_NAME);
	}

	public static String findCurrentSchema(DatabaseService databaseService, Connection connection) {
		return databaseService.executeNativeQuery(CURRENT_SCHEMA_QUERY, null, String.class, connection).getFirst();
	}

}
