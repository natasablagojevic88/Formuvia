package rs.formuvia.database.utils;

import java.io.BufferedWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.utils.StringUtils;

@RequiredArgsConstructor
public class CreateTable implements ExecuteQuery<Void> {

	private final TableInfo tableInfo;
	private static final String NUMERIC_TYPE = "numeric";
	private static final String BOOLEAN_TYPE = "bool";
	private static final String INTEGER_TYPE = "int4";
	private static final String STRING_TYPE = "varchar";
	private static final String DATE_TYPE = "date";
	private static final String DATE_TIME_TYPE = "timestamp";
	private static final String LONG_TYPE = "int8";
	public static final String ID_DEFAULT = "uuidv7()";
	private static final String UUID_DEFAULT = "uuid";
	private static final String COLUMN_NAME_REPLACE = "#columnname#";
	private static final String LIST_OF_VALUES_REPLACE = "#list_of_values#";
	private static final String CHECK_CONSTRAINT_QUERY = "CHECK (((#columnname#)::text = ANY (ARRAY[#list_of_values#])))";

	private DatabaseService databaseService = new DatabaseServiceImpl();

	@Override
	public Void execute(Connection connection) throws Exception {
		String query = createTableQuery();
		databaseService.executeUpdateQuery(query, null, connection);
		return null;
	}

	private String createTableQuery() throws Exception {
		StringWriter stringWriter = new StringWriter();
		BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);
		bufferedWriter.write("CREATE TABLE");
		bufferedWriter.newLine();
		bufferedWriter.write(this.tableInfo.getName());
		bufferedWriter.newLine();
		bufferedWriter.write("(");
		bufferedWriter.newLine();
		int index = 0;
		for (ColumnInfo columnInfo : this.tableInfo.getColumns()) {
			index++;
			bufferedWriter.write(createColumnQuery(columnInfo));
			if (index != this.tableInfo.getColumns().size())
				bufferedWriter.write(",");

			bufferedWriter.newLine();
		}

		bufferedWriter.write(")");
		bufferedWriter.close();
		return stringWriter.toString();
	}

	public static String createColumnQuery(ColumnInfo columnInfo) throws Exception {
		StringWriter stringWriter = new StringWriter();
		BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);
		bufferedWriter.write(columnInfo.getName());
		bufferedWriter.write(" ");
		if (StringUtils.hasText(columnInfo.getColumnDefinition())) {
			bufferedWriter.write(columnInfo.getColumnDefinition());
		} else {
			switch (columnInfo.getColumnType()) {
			case BIGDECIMAL:
				bufferedWriter.write(NUMERIC_TYPE + "(" + columnInfo.getLength() + "," + columnInfo.getScale() + ")");
				break;
			case BOOLEAN:
				bufferedWriter.write(BOOLEAN_TYPE);
				break;
			case INTEGER:
				bufferedWriter.write(INTEGER_TYPE);
				break;
			case LOCALDATE:
				bufferedWriter.write(DATE_TYPE);
				break;
			case LOCALDATETIME:
				bufferedWriter.write(DATE_TIME_TYPE);
				break;
			case LONG:
				bufferedWriter.write(LONG_TYPE);
				break;
			case STRING:
				if (!columnInfo.getIsPrimary())
					bufferedWriter.write(STRING_TYPE + "(" + columnInfo.getLength() + ")");
				break;
			case UUID:
				bufferedWriter.write(UUID_DEFAULT);
				break;

			}
		}
		if (columnInfo.getIsPrimary()) {
			bufferedWriter
					.write(" DEFAULT " + ID_DEFAULT + " CONSTRAINT " + columnInfo.getTableName() + "_pk PRIMARY key");
		}

		if (!columnInfo.getListOfValues().isEmpty()) {
			bufferedWriter.write(" ");
			bufferedWriter.write(" CONSTRAINT ");
			bufferedWriter.write(createCheckConstraintEnumName(columnInfo));
			bufferedWriter.write(" ");
			bufferedWriter.write(createListOfValuesConstraint(columnInfo.getName(), columnInfo.getListOfValues()));
		}

		if (columnInfo.getNullable())
			bufferedWriter.write(" NULL");
		else
			bufferedWriter.write(" NOT NULL");

		bufferedWriter.close();
		return stringWriter.toString();
	}

	public static String createCheckConstraintEnumName(ColumnInfo columnInfo) {
		return columnInfo.getTableName() + "_" + columnInfo.getName() + "_check";
	}

	public static String createListOfValuesConstraint(String columnName, List<String> listOfValues) {
		String values = new String(CHECK_CONSTRAINT_QUERY.getBytes(StandardCharsets.UTF_8));
		values = values.replace(COLUMN_NAME_REPLACE, columnName);
		values = values.replace(LIST_OF_VALUES_REPLACE, listOfValues.stream()
				.map(a -> "('" + a + "'::character varying)::text").collect(Collectors.joining(", ")));
		return values;
	}
}
