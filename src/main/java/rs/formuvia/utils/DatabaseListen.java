package rs.formuvia.utils;

import rs.formuvia.administration.entity.AppUser;
import rs.formuvia.administration.entity.AppUserRole;
import rs.formuvia.administration.entity.Role;
import rs.formuvia.model.entity.Model;
import rs.formuvia.model.entity.ModelColumn;
import rs.formuvia.model.entity.ModelColumnCondition;
import rs.formuvia.model.entity.ModelFile;
import rs.formuvia.model.entity.ModelJasper;
import rs.formuvia.model.entity.ModelProcedure;
import rs.formuvia.model.entity.ModelTrigger;

public enum DatabaseListen {

	listen_appuser(AppUser.class), listen_appuser_role(AppUserRole.class), listen_role(Role.class),
	listen_model(Model.class), listen_model_column(ModelColumn.class), listen_model_file(ModelFile.class),
	listen_model_column_condition(ModelColumnCondition.class), listen_model_procedure(ModelProcedure.class),
	listen_model_trigger(ModelTrigger.class), listen_model_jasper(ModelJasper.class);

	public Class<?> entityClass;

	private DatabaseListen(Class<?> entityClass) {
		this.entityClass = entityClass;
	}

}
