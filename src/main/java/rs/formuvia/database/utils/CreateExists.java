package rs.formuvia.database.utils;

import java.sql.Connection;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.SqlQueryWriterService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.service.impl.SqlQueryWriterServiceImpl;

@RequiredArgsConstructor
public class CreateExists<C> implements ExecuteQuery<Boolean> {
	private final DatabaseParameter databaseParameter;
	private final Class<C> resultClass;

	private DatabaseService databaseService = new DatabaseServiceImpl();
	private SqlQueryWriterService sqlQueryWriterService = new SqlQueryWriterServiceImpl();

	@Override
	public Boolean execute(Connection connection) throws Exception {
		QueryTableInfo queryTableInfo = GenerateQueryFromDTO.createQueryTableInfo(this.resultClass);
		GenerateQueryFromDTO.fillColumnType(databaseParameter, resultClass);
		String query = sqlQueryWriterService.createTotalQuery(queryTableInfo, databaseParameter);
		Map<Integer, Object> parameters = sqlQueryWriterService.createParameters(databaseParameter.getFilters());
		Long count = this.databaseService.executeNativeQuery(query, parameters, Long.class, connection).getFirst();
		return count == 0 ? false : true;
	}

}
