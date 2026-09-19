package rs.formuvia.database.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BaseConstraintInfo {

	private String table;
	
	private String name;
	
	private String type;
	
	private String definition;
}
