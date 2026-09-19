package rs.formuvia.database.utils;

import java.io.BufferedWriter;
import java.io.StringWriter;
import java.sql.Connection;

import lombok.RequiredArgsConstructor;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;

@RequiredArgsConstructor
public class CreateIndex implements ExecuteQuery<Void> {

	private final IndexInfo indexInfo;

	private DatabaseService databaseService = new DatabaseServiceImpl();

	@Override
	public Void execute(Connection connection) throws Exception {
		String query = createQuery(indexInfo);
		databaseService.executeUpdateQuery(query, null, connection);
		return null;
	}

	private String createQuery(IndexInfo indexInfo) throws Exception {
		StringWriter stringWriter = new StringWriter();
		BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);
		bufferedWriter.write("CREATE INDEX ");
		bufferedWriter.write(indexInfo.getName());
		bufferedWriter.newLine();
		bufferedWriter.write("ON ");
		bufferedWriter.write(indexInfo.getTableName());
		bufferedWriter.newLine();
		bufferedWriter.write("USING btree(");
		bufferedWriter.write(indexInfo.getColumnName());
		bufferedWriter.write(")");

		bufferedWriter.close();
		return stringWriter.toString();
	}
}
