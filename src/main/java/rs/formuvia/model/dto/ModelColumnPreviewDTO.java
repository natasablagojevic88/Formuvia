package rs.formuvia.model.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.common.dto.ComboboxDTO;
import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.database.utils.ParentListOfValues;

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

	private Boolean showable;

	private Object value;

	private UUID modelId;

	private List<ComboboxDTO> listOfValues = new ArrayList<>();

	private List<ParentListOfValues> parentListOfValues = new ArrayList<>();

	private Boolean textArea;

	private Integer rowIndex;

	private Integer columnIndex;

	private Integer colspan;

	private List<ModelColumnConditionDTO> conditions = new ArrayList<>();

}
