package rs.formuvia.database.utils;

import java.sql.Connection;

import rs.formuvia.administration.dto.AppUserRoleDTO;
import rs.formuvia.administration.dto.RoleDTO;
import rs.formuvia.administration.entity.AppUser;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.utils.StaticData;

public class LoadStaticData implements ExecuteQuery<Void> {

	private DatabaseService databaseService = new DatabaseServiceImpl();

	@Override
	public Void execute(Connection connection) throws Exception {
		StaticData.appUsers = this.databaseService.findAll(null, AppUser.class, connection);
		StaticData.appUserRoles = this.databaseService.findAll(null, AppUserRoleDTO.class, connection);
		StaticData.roles = this.databaseService.findAll(null, RoleDTO.class, connection);
		return null;
	}

}
