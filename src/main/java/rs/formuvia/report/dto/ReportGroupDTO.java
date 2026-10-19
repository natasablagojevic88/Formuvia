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
import rs.formuvia.report.entity.ReportGroup;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.RoleList;

@Setter
@Getter
@EntityClass(value = ReportGroup.class, roles = RoleList.ADMIN, saveUrl = ApiRoute.reportGroup)
@NoArgsConstructor
@AllArgsConstructor
public class ReportGroupDTO {

	@HideInTable
	private UUID id;

	@NotNull
	@InitSort
	private String name;

	@NotNull
	@HideInTable
	private UUID roleId;

	private String roleCode;

	private String icon;
}
