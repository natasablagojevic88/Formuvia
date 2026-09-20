package rs.formuvia.administration.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Inject;
import rs.formuvia.administration.dto.AppUserDTO;
import rs.formuvia.administration.dto.AppUserRoleDTO;
import rs.formuvia.administration.dto.RoleDTO;
import rs.formuvia.administration.entity.AppUser;
import rs.formuvia.administration.service.AppUserService;
import rs.formuvia.administration.utils.UpdateAppUser;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.utils.StaticData;

@Service
public class AppUserServiceImpl implements AppUserService {

	@Inject
	private DatabaseService databaseService;

	@Override
	public DatabaseTable<AppUserDTO> getTable(DatabaseParameter databaseParameter) {

		DatabaseTable<AppUserDTO> databaseTable = databaseService.createTable(databaseParameter, AppUserDTO.class);
		databaseTable.getList().forEach(a -> {
			a.setPassword(null);
			a.setUserRoles(rolesForUser(a.getId()));
		});
		return databaseTable;
	}

	@Override
	public AppUserDTO getAppUserDTO(UUID id) {
		AppUserDTO appUserDTO = databaseService.findById(id, AppUserDTO.class);
		appUserDTO.setPassword(null);
		appUserDTO.setAllRoles(StaticData.roles.stream().collect(Collectors.toList()));

		appUserDTO.setUserRoles(rolesForUser(id));

		return appUserDTO;
	}

	private List<RoleDTO> rolesForUser(UUID id) {
		List<AppUserRoleDTO> rolesForUser = this.databaseService.findAll(
				DatabaseParameter.valueOf(DatabaseFilter.valueOf("appUserId", id.toString())), AppUserRoleDTO.class);
		return rolesForUser.stream().map(a -> new RoleDTO(a.getRoleId(), a.getRoleCode(), a.getRoleDescription()))
				.toList();
	}

	@Override
	public AppUserDTO getUpdate(AppUserDTO appUserDTO) {
		AppUser appUser = this.databaseService.executeQuery(new UpdateAppUser(appUserDTO));

		return getAppUserDTO(appUser.getId());
	}

	@Override
	public void getDelete(UUID uuid) {
		this.databaseService.delete(this.databaseService.findById(uuid, AppUser.class));

	}

	@Override
	public List<RoleDTO> getAllRoles() {
		
		return StaticData.roles.stream().collect(Collectors.toList());
	}

}
