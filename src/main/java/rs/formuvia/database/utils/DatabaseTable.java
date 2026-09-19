package rs.formuvia.database.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseTable<C> {
	
	private String name;
	
	private List<DatabaseColumn> column=new ArrayList<>();

	private List<C> list = new ArrayList<>();
	
	private Long total;
	
	private Integer numberOfPages;
	
	private List<DatabaseColumn> allColumns=new ArrayList<>();
	
}
