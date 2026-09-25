package rs.formuvia.database.utils;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.common.dto.ComboboxDTO;
import rs.formuvia.database.enums.ColumnType;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseColumn {

	private String fieldName;

	private String description;

	private ColumnType columnType;

	private Boolean editable = true;

	private Boolean required = false;

	List<ComboboxDTO> listOfValues = new ArrayList<>();
}
