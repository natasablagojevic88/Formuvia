package rs.formuvia.report.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.database.annotations.HideInTable;
import rs.formuvia.database.annotations.InitSort;
import rs.formuvia.report.entity.Report;
import rs.formuvia.report.enums.ReportType;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.RoleList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityClass(value = Report.class, roles = RoleList.ADMIN, saveUrl = ApiRoute.report)
public class ReportDTO {

	@HideInTable
	private UUID id;

	@NotNull
	@HideInTable
	private UUID reportGroupId;

	@NotNull
	private ReportType type;

	@InitSort
	@NotNull
	private String name;

	private String description;

	@HideInTable
	private String sqlQuery;
}
