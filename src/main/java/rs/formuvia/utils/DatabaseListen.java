package rs.formuvia.utils;

import rs.formuvia.administration.entity.AppUser;
import rs.formuvia.administration.entity.AppUserRole;
import rs.formuvia.administration.entity.Role;
import rs.formuvia.model.entity.Model;
import rs.formuvia.model.entity.ModelColumn;

public enum DatabaseListen {

	listen_appuser(AppUser.class), listen_appuser_role(AppUserRole.class), listen_role(Role.class),
	listen_model(Model.class), listen_model_column(ModelColumn.class),;

	public Class<?> entityClass;

	private DatabaseListen(Class<?> entityClass) {
		this.entityClass = entityClass;
	}

}
