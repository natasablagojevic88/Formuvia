package rs.formuvia.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.database.enums.ColumnType;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReportResultColumnDTO {

	private String code;

	private String name;

	private ColumnType columnType;
}
