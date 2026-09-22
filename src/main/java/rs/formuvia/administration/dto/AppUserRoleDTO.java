package rs.formuvia.administration.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.administration.entity.AppUserRole;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.database.annotations.InitSort;
import rs.formuvia.utils.RoleList;

@Setter
@Getter
@EntityClass(value = AppUserRole.class, roles = RoleList.ADMIN)
@NoArgsConstructor
@AllArgsConstructor
public class AppUserRoleDTO {

	private UUID id;

	private UUID appUserId;

	private String appUserUsername;

	private UUID roleId;

	@InitSort
	private String roleCode;

	private String roleDescription;
}
