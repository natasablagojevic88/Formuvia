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
public class QueryTableFinalInfo {

	private String name;

	private List<QueryColumnFinalInfo> columns = new ArrayList<>();

	private List<QueryLeftJoin> leftJoins = new ArrayList<>();
}
