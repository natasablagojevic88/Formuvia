package rs.formuvia.report.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.report.enums.ReportType;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReportInfoDTO {

	private String title;

	private String description;

	private ReportType reportType;

	private List<ReportInfoParameterDTO> parameters = new ArrayList<>();
}
