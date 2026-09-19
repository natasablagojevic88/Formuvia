package rs.formuvia.administration.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.administration.entity.AppUser;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.database.annotations.HideInTable;
import rs.formuvia.utils.RoleList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityClass(value = AppUser.class, roles = RoleList.ADMIN)
public class AppUserDTO {

	@HideInTable
	private UUID id;

	@NotNull
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
}
