package rs.formuvia.database.service;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.database.utils.ExecuteQuery;

public interface DatabaseService {

	<C> List<C> executeNativeQuery(String query, Map<Integer, Object> parameters, Class<C> resultClass);

	<C> List<C> executeNativeQuery(String query, Map<Integer, Object> parameters, Class<C> resultClass,
			Connection connection);

	void executeUpdateQuery(String query, Map<Integer, Object> parameters);

	void executeUpdateQuery(String query, Map<Integer, Object> parameters, Connection connection);

	<C> C executeQuery(ExecuteQuery<C> executeQuery);

	<C> C executeQuery(ExecuteQuery<C> executeQuery, Connection connection);

	UUID generateUUID();

	UUID generateUUID(Connection connection);

	<C> List<C> findAll(DatabaseParameter databaseParameter, Class<C> resultClass);

	<C> List<C> findAll(DatabaseParameter databaseParameter, Class<C> resultClass, Connection connection);

	<C> C findById(UUID id, Class<C> resultClass);

	<C> C findById(UUID id, Class<C> resultClass, Connection connection);
	
	<C> DatabaseTable<C> createTable(DatabaseParameter databaseParameter, Class<C> resultClass);
	
	<C> DatabaseTable<C> createTable(DatabaseParameter databaseParameter, Class<C> resultClass, Connection connection);
	
	<C> C save(C entity);
	
	<C> C save(C entity, Connection connection);
	
	<C> void delete(C entity);
	
	<C> void delete(C entity, Connection connection);
	
	<C> Boolean exists(DatabaseParameter databaseParameter, Class<C> resultClass);
	
	<C> Boolean exists(DatabaseParameter databaseParameter, Class<C> resultClass, Connection connection);
}
