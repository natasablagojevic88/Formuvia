package rs.formuvia.database.utils;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.database.enums.ColumnType;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QueryColumnInfo {

	private String fieldName;

	private String columnName;

	private ColumnType columnType;

	private List<String> paths = new ArrayList<>();

	private List<String> tableName = new ArrayList<>();

	public static QueryColumnInfo valueOf(String fieldName, String columnName, ColumnType columnType) {
		QueryColumnInfo queryColumnInfo = new QueryColumnInfo();
		queryColumnInfo.setFieldName(fieldName);
		queryColumnInfo.setColumnName(columnName);
		queryColumnInfo.setColumnType(columnType);
		return queryColumnInfo;
	}

}
