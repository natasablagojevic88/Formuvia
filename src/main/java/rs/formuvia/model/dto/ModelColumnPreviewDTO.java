package rs.formuvia.model.dto;

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
public class ModelColumnPreviewDTO {

	private String code;

	private String name;

	private ColumnType columnType;

	private Integer length;

	private Boolean nullable;

	private Boolean editable;

	private Object value;

	private List<ComboboxDTO> listOfValues = new ArrayList<>();

	private Boolean textArea;

	private Integer rowIndex;

	private Integer columnIndex;

	private Integer colspan;

}
