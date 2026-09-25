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
public class ColumnInfo {

	private String name;

	private String tableName;

	private Boolean nullable = false;

	private Boolean isPrimary = false;

	private ColumnType columnType;

	private List<String> listOfValues = new ArrayList<>();

	private Integer length;

	private Integer scale;

	private String columnDefinition;
}
