package rs.formuvia.database.utils;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.SqlQueryWriterService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.service.impl.SqlQueryWriterServiceImpl;

@RequiredArgsConstructor
public class CreateDelete<C> implements ExecuteQuery<Void> {

	private final C entity;

	private DatabaseService databaseService = new DatabaseServiceImpl();
	private SqlQueryWriterService sqlQueryWriterService = new SqlQueryWriterServiceImpl();

	@Override
	public Void execute(Connection connection) throws Exception {
		String tableName = CreateSave.findTableName(entity);
		UUID id = CreateSave.findId(entity);
		databaseService.findById(id, entity.getClass(), connection);
		String deleteQuery = sqlQueryWriterService.deleteQuery(tableName);
		Map<Integer, Object> parameters = new HashMap<>();
		parameters.put(1, id);
		this.databaseService.executeUpdateQuery(deleteQuery, parameters, connection);

		return null;
	}

}
