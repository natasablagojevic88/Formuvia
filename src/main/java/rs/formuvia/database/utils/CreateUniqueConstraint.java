package rs.formuvia.database.utils;

import java.io.BufferedWriter;
import java.io.StringWriter;
import java.sql.Connection;

import lombok.RequiredArgsConstructor;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.utils.StringUtils;

@RequiredArgsConstructor
public class CreateUniqueConstraint implements ExecuteQuery<Void> {

	private final UniqueConstraintInfo uniqueConstraintInfo;

	private DatabaseService databaseService = new DatabaseServiceImpl();

	@Override
	public Void execute(Connection connection) throws Exception {
		String query = createQuery(uniqueConstraintInfo);
		databaseService.executeUpdateQuery(query, null, connection);
		return null;
	}

	private String createQuery(UniqueConstraintInfo uniqueConstraintInfo) throws Exception {
		StringWriter stringWriter = new StringWriter();
		BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);
		bufferedWriter.write("ALTER TABLE ");
		bufferedWriter.write(uniqueConstraintInfo.getTableName());
		bufferedWriter.newLine();
		bufferedWriter.write("ADD CONSTRAINT ");
		bufferedWriter.write(uniqueConstraintInfo.getName());
		bufferedWriter.write(" UNIQUE (");
		bufferedWriter.write(StringUtils.createStringArray(uniqueConstraintInfo.getColumns()));
		bufferedWriter.write(")");
		bufferedWriter.close();
		return stringWriter.toString();
	}
}
