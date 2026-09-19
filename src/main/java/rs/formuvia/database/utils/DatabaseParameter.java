package rs.formuvia.database.utils;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseParameter {

	private Integer pageIndex = 0;
	private Integer pageSize = Integer.MAX_VALUE;

	private List<DatabaseFilter> filters = new ArrayList<>();

	private List<QueryDatabaseOrder> orders = new ArrayList<>();

	public static DatabaseParameter valueOf(DatabaseFilter...databaseFilters) {
		DatabaseParameter databaseParameter = new DatabaseParameter();
		for (DatabaseFilter filter : databaseFilters) {
			databaseParameter.getFilters().add(filter);
		}
		return databaseParameter;
	}
}
