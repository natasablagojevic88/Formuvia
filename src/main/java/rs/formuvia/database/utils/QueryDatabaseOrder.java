package rs.formuvia.database.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.database.enums.Direction;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QueryDatabaseOrder {

	private String fieldName;

	private Direction direction;

	public static QueryDatabaseOrder valueOf(String fieldName, Direction direction) {
		return new QueryDatabaseOrder(fieldName, direction);
	}

}
