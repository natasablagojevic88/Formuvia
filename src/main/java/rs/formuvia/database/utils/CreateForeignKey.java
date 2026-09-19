package rs.formuvia.database.utils;

import java.io.BufferedWriter;
import java.io.StringWriter;
import java.sql.Connection;

import lombok.RequiredArgsConstructor;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;

@RequiredArgsConstructor
public class CreateForeignKey implements ExecuteQuery<Void> {
	private final ForeignKeyInfo foreignKeyInfo;

	private DatabaseService databaseService = new DatabaseServiceImpl();
	private static final String ID_COLUMN_NAME = "id";

	@Override
	public Void execute(Connection connection) throws Exception {
		String query = getQuery(foreignKeyInfo);
		databaseService.executeUpdateQuery(query, null, connection);
		return null;
	}

	private String getQuery(ForeignKeyInfo foreignKeyInfo) throws Exception {
		StringWriter stringWriter = new StringWriter();
		BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);
		bufferedWriter.write("ALTER TABLE ");
		bufferedWriter.write(foreignKeyInfo.getTableName());
		bufferedWriter.newLine();
		bufferedWriter.write("ADD CONSTRAINT ");
		bufferedWriter.write(foreignKeyInfo.getName());
		bufferedWriter.newLine();
		bufferedWriter.write("FOREIGN KEY (");
		bufferedWriter.write(foreignKeyInfo.getColumnName());
		bufferedWriter.write(")");
		bufferedWriter.newLine();
		bufferedWriter.write("REFERENCES ");
		bufferedWriter.write(foreignKeyInfo.getReferenceTable());
		bufferedWriter.write("(");
		bufferedWriter.write(ID_COLUMN_NAME);
		bufferedWriter.write(")");
		if (foreignKeyInfo.getCascadeDelete()) {
			bufferedWriter.newLine();
			bufferedWriter.write("ON DELETE CASCADE");
		}
		bufferedWriter.close();
		return stringWriter.toString();
	}

}
