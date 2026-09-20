package rs.formuvia.administration.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.administration.entity.Role;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.database.annotations.HideInTable;
import rs.formuvia.database.annotations.InitSort;
import rs.formuvia.database.annotations.NotEditableInTable;
import rs.formuvia.utils.RoleList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityClass(value=Role.class, roles=RoleList.ADMIN)
public class RoleDTO {

	@HideInTable
	@NotEditableInTable
	private UUID id;
	
	@InitSort
	@NotNull
	private String code;

	private String description;
}
