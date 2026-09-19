package rs.formuvia.database.utils;

import java.io.BufferedWriter;
import java.io.StringWriter;
import java.sql.Connection;

import lombok.RequiredArgsConstructor;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;

@RequiredArgsConstructor
public class CreateColumn implements ExecuteQuery<Void> {

	private final ColumnInfo columnInfo;

	private DatabaseService databaseService = new DatabaseServiceImpl();

	@Override
	public Void execute(Connection connection) throws Exception {
		String query = createQuery(columnInfo);
		databaseService.executeUpdateQuery(query, null, connection);
		return null;
	}

	private String createQuery(ColumnInfo columnInfo) throws Exception {
		StringWriter stringWriter = new StringWriter();
		BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);

		bufferedWriter.write("ALTER TABLE " + columnInfo.getTableName());
		bufferedWriter.newLine();
		bufferedWriter.write("ADD ");
		bufferedWriter.write(CreateTable.createColumnQuery(columnInfo));

		bufferedWriter.close();
		return stringWriter.toString();
	}

}
