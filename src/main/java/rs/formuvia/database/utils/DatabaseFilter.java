package rs.formuvia.database.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.database.enums.SearchOperation;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseFilter {

	public static DatabaseFilter valueOf(String field, SearchOperation searchOperation, String field1, String field2) {
		DatabaseFilter databaseFilter = new DatabaseFilter();
		databaseFilter.setField(field);
		databaseFilter.setSearchOperation(searchOperation);
		databaseFilter.setField1(field1);
		databaseFilter.setField2(field2);
		return databaseFilter;
	}

	public static DatabaseFilter valueOf(String field, SearchOperation searchOperation, String field1) {
		DatabaseFilter databaseFilter = new DatabaseFilter();
		databaseFilter.setField(field);
		databaseFilter.setSearchOperation(searchOperation);
		databaseFilter.setField1(field1);
		return databaseFilter;
	}

	public static DatabaseFilter valueOf(String field, String field1) {
		DatabaseFilter databaseFilter = new DatabaseFilter();
		databaseFilter.setField(field);
		databaseFilter.setSearchOperation(SearchOperation.EQUALS);
		databaseFilter.setField1(field1);
		return databaseFilter;
	}

	private String field;

	private SearchOperation searchOperation;

	private ColumnType columnType;

	private String field1;

	private String field2;
}
