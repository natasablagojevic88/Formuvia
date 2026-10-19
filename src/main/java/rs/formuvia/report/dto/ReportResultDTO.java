package rs.formuvia.report.dto;

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
public class ReportResultDTO {

	private List<ReportResultColumnDTO> columns = new ArrayList<>();

	private List<LinkedHashMap<String, Object>> list = new ArrayList<>();

}
