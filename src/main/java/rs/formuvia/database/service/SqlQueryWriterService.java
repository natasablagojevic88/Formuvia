package rs.formuvia.database.service;

import java.util.List;
import java.util.Map;

import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.QueryTableInfo;

public interface SqlQueryWriterService {

	String createSelectQuery(QueryTableInfo queryTableInfo, DatabaseParameter databaseParameter);

	Map<Integer, Object> createParameters(List<DatabaseFilter> databaseFilters);

	String createTotalQuery(QueryTableInfo queryTableInfo, DatabaseParameter databaseParameter);

	String insertQuery(String[] fieldArray, String tableName);

	String updateQuery(String[] fieldArray, String tableName);

	String deleteQuery(String tableName);
}
