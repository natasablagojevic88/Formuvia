package rs.formuvia.database.utils;

import java.sql.Connection;

import lombok.RequiredArgsConstructor;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;

@RequiredArgsConstructor
public class CreateEnumCheckConstraint implements ExecuteQuery<Void> {

	private final ColumnInfo columnInfo;
	private final String constraintNameReplace = "#constraint_name#";
	private final String tableNameReplace = "#table_name#";
	private final String constraintTextReplace = "#constraint_text#";
	private final String dropConstraintIfExists = "alter table #table_name# drop constraint if exists #constraint_name#";
	private final String addConstraint = "alter table #table_name# add constraint #constraint_name# #constraint_text#";

	private DatabaseService databaseService = new DatabaseServiceImpl();

	@Override
	public Void execute(Connection connection) throws Exception {
		String constraintName = CreateTable.createCheckConstraintEnumName(columnInfo);
		String constraintText = CreateTable.createListOfValuesConstraint(columnInfo.getName(),
				columnInfo.getListOfValues());

		String dropConstraintIfExitsQuery = new String(dropConstraintIfExists.getBytes());
		dropConstraintIfExitsQuery = dropConstraintIfExitsQuery.replace(tableNameReplace, columnInfo.getTableName());
		dropConstraintIfExitsQuery = dropConstraintIfExitsQuery.replace(constraintNameReplace, constraintName);
		databaseService.executeUpdateQuery(dropConstraintIfExitsQuery, null, connection);

		String addConstraintQuery = new String(addConstraint.getBytes());
		addConstraintQuery = addConstraintQuery.replace(tableNameReplace, columnInfo.getTableName());
		addConstraintQuery = addConstraintQuery.replace(constraintNameReplace, constraintName);
		addConstraintQuery = addConstraintQuery.replace(constraintTextReplace, constraintText);
		databaseService.executeUpdateQuery(addConstraintQuery, null, connection);

		return null;
	}
}
