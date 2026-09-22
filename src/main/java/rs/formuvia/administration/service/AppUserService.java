package rs.formuvia.administration.service;

import java.util.List;
import java.util.UUID;

import rs.formuvia.administration.dto.AppUserDTO;
import rs.formuvia.administration.dto.RoleDTO;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;

public interface AppUserService {

	DatabaseTable<AppUserDTO> getTable(DatabaseParameter databaseParameter);

	AppUserDTO getAppUserDTO(UUID id);

	AppUserDTO getUpdate(AppUserDTO appUserDTO);

	void getDelete(UUID uuid);

	List<RoleDTO> getAllRoles();
}
