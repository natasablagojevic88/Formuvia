package rs.formuvia.utils;

import rs.formuvia.administration.entity.AppUser;
import rs.formuvia.administration.entity.AppUserRole;

public enum DatabaseListen {

	appuser_listen(AppUser.class),
	appuser_role_listen(AppUserRole.class),
	;
	
	public Class<?> entityClass;

	private DatabaseListen(Class<?> entityClass) {
		this.entityClass = entityClass;
	}
	
	
}
