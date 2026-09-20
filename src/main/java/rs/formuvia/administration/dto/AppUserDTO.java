package rs.formuvia.administration.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.administration.entity.AppUser;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.database.annotations.HideInTable;
import rs.formuvia.database.annotations.InitSort;
import rs.formuvia.database.annotations.NotEditableInTable;
import rs.formuvia.database.annotations.SkipColumn;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.RoleList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityClass(value = AppUser.class, roles = RoleList.ADMIN, saveUrl = ApiRoute.appuser)
public class AppUserDTO {

	@HideInTable
	@NotEditableInTable
	private UUID id;

	@NotNull
	@InitSort
	private String username;

	@HideInTable
	private String password;

	@NotNull
	private String name;

	@NotNull
	private String surname;

	@NotNull
	private String email;

	private Boolean active;
	
	@SkipColumn
	@HideInTable
	private List<RoleDTO> userRoles=new ArrayList<>();
	
	@SkipColumn
	@HideInTable
	private List<RoleDTO> allRoles=new ArrayList<>();
}
