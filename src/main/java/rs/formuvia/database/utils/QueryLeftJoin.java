package rs.formuvia.database.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QueryLeftJoin {
	
	private String path;

	private String prefix;
	
	private String tableName;
	
	private String referenceTable;
}
