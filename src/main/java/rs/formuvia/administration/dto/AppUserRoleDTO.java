package rs.formuvia.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.administration.entity.AppUserRole;
import rs.formuvia.database.annotations.EntityClass;

@Setter
@Getter
@EntityClass(AppUserRole.class)
@NoArgsConstructor
@AllArgsConstructor
public class AppUserRoleDTO {

	private String appUserUsername;
	
	private String roleCode;
}
