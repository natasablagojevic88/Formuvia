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
import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.report.entity.ReportParameter;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.RoleList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityClass(value = ReportParameter.class, roles = RoleList.ADMIN, saveUrl = ApiRoute.reportParameter)
public class ReportParameterDTO {

	@HideInTable
	private UUID id;

	@HideInTable
	@NotNull
	private UUID reportId;

	@NotNull
	@InitSort
	private Integer orderNumber;

	@NotNull
	private ColumnType columnType;

	@NotNull
	private String code;

	@NotNull
	private String name;

	@NotNull
	private Boolean nullable;

	@HideInTable
	private String defaultValue;

	@HideInTable
	private String listOfValues;
}
