package rs.formuvia.report.dto;

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
public class ReportInfoParameterDTO {

	private String code;

	private String name;

	private ColumnType type;

	private Object defaultValue;

	private List<ComboboxDTO> listOfValues = new ArrayList<>();
}
