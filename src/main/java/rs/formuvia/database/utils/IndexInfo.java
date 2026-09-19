package rs.formuvia.database.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IndexInfo {

	private String name;
	
	private String tableName;
	
	private String columnName;
}
