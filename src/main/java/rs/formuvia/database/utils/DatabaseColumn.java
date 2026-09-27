package rs.formuvia.database.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

	private Boolean inDescription = false;

	private UUID modelId;

	List<ComboboxDTO> listOfValues = new ArrayList<>();

	List<ParentListOfValues> parentList = new ArrayList<>();

}
