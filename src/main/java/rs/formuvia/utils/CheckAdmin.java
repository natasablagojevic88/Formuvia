package rs.formuvia.utils;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.List;
import java.util.stream.Collectors;

import at.favre.lib.crypto.bcrypt.BCrypt;
import rs.formuvia.administration.dto.AppUserRoleDTO;
import rs.formuvia.administration.entity.AppUser;
import rs.formuvia.administration.entity.AppUserRole;
import rs.formuvia.administration.entity.Role;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.ExecuteQuery;

public class CheckAdmin implements ExecuteQuery<Void> {

	private DatabaseService databaseService = new DatabaseServiceImpl();

	private static final String DEFAULT_ADMIN_PASSWORD = "admin.default.password";
	private static final String DEFAULT_ADMIN_NAME = "System";
	private static final String DEFAULT_ADMIN_SURNAME = "Administrator";
	private static final String DEFAULT_ADMIN_USERNAME = "admin";
	public static final Integer BCRYPT_CODE = 12;

	@Override
	public Void execute(Connection connection) throws Exception {
		DatabaseParameter databaseParameter = DatabaseParameter
				.valueOf(DatabaseFilter.valueOf("username", DEFAULT_ADMIN_USERNAME));

		if (!this.databaseService.exists(databaseParameter, AppUser.class, connection)) {
			AppUser appUser = new AppUser();
			appUser.setActive(true);
			appUser.setName(DEFAULT_ADMIN_NAME);
			String defaultPassword = StaticData.appProperties.getProperty(DEFAULT_ADMIN_PASSWORD);
			appUser.setPassword(BCrypt.withDefaults().hashToString(BCRYPT_CODE, defaultPassword.toCharArray()));
			appUser.setSurname(DEFAULT_ADMIN_SURNAME);
			appUser.setUsername(DEFAULT_ADMIN_USERNAME);
			this.databaseService.save(appUser, connection);
		}

		List<Role> roles = this.databaseService.findAll(null, Role.class, connection);

		List<Field> roleFields = StaticData.classFields.get(RoleList.class).stream().collect(Collectors.toList());

		for (Field field : roleFields) {
			String roleCode = field.get(new RoleList()).toString();

			if (roles.stream().filter(a -> a.getCode().equals(roleCode)).count() != 0) {
				continue;
			}

			Role role = new Role(null, roleCode, null);
			this.databaseService.save(role, connection);
		}

		DatabaseParameter databaseParameterRole = DatabaseParameter
				.valueOf(new DatabaseFilter[] { DatabaseFilter.valueOf("appUserUsername", DEFAULT_ADMIN_USERNAME),
						DatabaseFilter.valueOf("roleCode", RoleList.ADMIN) });
		if (!this.databaseService.exists(databaseParameterRole, AppUserRoleDTO.class, connection)) {
			AppUserRole appUserRole = new AppUserRole();
			appUserRole.setAppUser(this.databaseService
					.findAll(DatabaseParameter.valueOf(DatabaseFilter.valueOf("username", DEFAULT_ADMIN_USERNAME)),
							AppUser.class, connection)
					.getFirst());
			appUserRole.setRole(this.databaseService
					.findAll(DatabaseParameter.valueOf(DatabaseFilter.valueOf("code", RoleList.ADMIN)), Role.class,
							connection)
					.getFirst());
			this.databaseService.save(appUserRole, connection);
		}
		return null;
	}

}
