package rs.formuvia.model.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.database.annotations.InitSort;
import rs.formuvia.model.entity.ModelProcedure;
import rs.formuvia.utils.RoleList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityClass(value = ModelProcedure.class, roles = RoleList.ADMIN)
public class ModelProcedureDTO {

	private UUID id;

	@NotNull
	@InitSort
	private String name;

	@NotNull
	private UUID modelId;

	private String icon;

	@NotNull
	private String procedure;
}