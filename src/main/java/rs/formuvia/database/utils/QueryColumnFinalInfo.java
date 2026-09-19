package rs.formuvia.database.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QueryColumnFinalInfo {

	private String fieldName;

	private String columnName;

	private String columnPrefix;

}
