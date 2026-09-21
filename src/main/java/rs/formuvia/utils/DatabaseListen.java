package rs.formuvia.utils;

import rs.formuvia.administration.entity.AppUser;
import rs.formuvia.administration.entity.AppUserRole;
import rs.formuvia.administration.entity.Role;
import rs.formuvia.model.entity.Model;

public enum DatabaseListen {

	appuser_listen(AppUser.class),
	appuser_role_listen(AppUserRole.class),
	role_listen(Role.class),
	model_listen(Model.class),
	;
	
	public Class<?> entityClass;

	private DatabaseListen(Class<?> entityClass) {
		this.entityClass = entityClass;
	}
	
	
}
