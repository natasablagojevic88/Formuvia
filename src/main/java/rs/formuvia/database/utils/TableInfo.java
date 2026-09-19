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
public class TableInfo {

	private String name;

	private List<ColumnInfo> columns = new ArrayList<>();

	private List<UniqueConstraintInfo> uniqueContraints = new ArrayList<>();

	private List<IndexInfo> indexes = new ArrayList<>();

	private List<ForeignKeyInfo> foreignKeys = new ArrayList<>();
}
