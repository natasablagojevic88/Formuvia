package rs.formuvia.administration.utils;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;

import org.modelmapper.ModelMapper;

import at.favre.lib.crypto.bcrypt.BCrypt;
import lombok.RequiredArgsConstructor;
import rs.formuvia.administration.dto.AppUserDTO;
import rs.formuvia.administration.dto.AppUserRoleDTO;
import rs.formuvia.administration.dto.RoleDTO;
import rs.formuvia.administration.entity.AppUser;
import rs.formuvia.administration.entity.AppUserRole;
import rs.formuvia.administration.entity.Role;
import rs.formuvia.database.enums.SearchOperation;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.exceptions.NotNullException;
import rs.formuvia.exceptions.UniqueException;
import rs.formuvia.utils.CheckAdmin;
import rs.formuvia.utils.StringUtils;

@RequiredArgsConstructor
public class UpdateAppUser implements ExecuteQuery<AppUser> {

	private final AppUserDTO appUserDTO;

	private DatabaseService databaseService = new DatabaseServiceImpl();
	private ModelMapper modelMapper = new ModelMapper();

	@Override
	public AppUser execute(Connection connection) throws Exception {
		AppUser appUser = this.appUserDTO.getId() == null ? new AppUser()
				: this.databaseService.findById(appUserDTO.getId(), AppUser.class, connection);
		if (appUserDTO.getId() == null && (!StringUtils.hasText(appUserDTO.getPassword()))) {
			throw new NotNullException(UniqueException.findFieldFromList(AppUserDTO.class, "password"));
		}

		if (appUser.getId() != null) {
			if (this.databaseService.exists(
					DatabaseParameter.valueOf(new DatabaseFilter[] {
							DatabaseFilter.valueOf("username", appUserDTO.getUsername()),
							DatabaseFilter.valueOf("id", SearchOperation.NOT_EQUALS, appUserDTO.getId().toString()) }),
					AppUser.class, connection)) {
				throw new UniqueException(UniqueException.findFieldFromList(AppUserDTO.class, "username"),
						appUserDTO.getUsername());
			}
		} else {
			if (this.databaseService.exists(
					DatabaseParameter.valueOf(
							new DatabaseFilter[] { DatabaseFilter.valueOf("username", appUserDTO.getUsername()) }),
					AppUser.class, connection)) {
				throw new UniqueException(UniqueException.findFieldFromList(AppUserDTO.class, "username"),
						appUserDTO.getUsername());
			}
		}

		if (StringUtils.hasText(appUserDTO.getPassword())) {
			appUserDTO.setPassword(convertPasswordToHash(appUserDTO.getPassword()));
		} else {
			appUserDTO.setPassword(appUser.getPassword());
		}

		modelMapper.map(appUserDTO, appUser);

		appUser = this.databaseService.save(appUser, connection);

		List<AppUserRoleDTO> currentRoles = rolesForUser(appUser.getId(), connection);

		List<String> choosenRole = appUserDTO.getUserRoles().stream().map(a -> a.getCode()).toList();
		List<String> currentRolesString = currentRoles.stream().map(a -> a.getRoleCode()).toList();

		List<AppUserRoleDTO> rolesForDelete = currentRoles.stream()
				.filter(a -> !(choosenRole.contains(a.getRoleCode()))).toList();

		for (AppUserRoleDTO appUserRoleDTO : rolesForDelete) {
			this.databaseService.delete(
					this.databaseService.findById(appUserRoleDTO.getId(), AppUserRole.class, connection), connection);
		}

		List<RoleDTO> rolesToInsert = this.appUserDTO.getUserRoles().stream()
				.filter(a -> !currentRolesString.contains(a.getCode())).toList();
		for (RoleDTO role : rolesToInsert) {
			AppUserRole appUserRole = new AppUserRole();
			appUserRole.setAppUser(appUser);
			appUserRole.setRole(databaseService.findById(role.getId(), Role.class, connection));
			this.databaseService.save(appUserRole, connection);
		}

		return appUser;
	}

	public static String convertPasswordToHash(String password) {
		return BCrypt.withDefaults().hashToString(CheckAdmin.BCRYPT_CODE, password.toCharArray());
	}

	private List<AppUserRoleDTO> rolesForUser(UUID id, Connection connection) {
		List<AppUserRoleDTO> rolesForUser = this.databaseService.findAll(
				DatabaseParameter.valueOf(DatabaseFilter.valueOf("appUserId", id.toString())), AppUserRoleDTO.class,
				connection);
		return rolesForUser;
	}

}
