package rs.formuvia.administration.service;

import java.util.UUID;

import rs.formuvia.administration.dto.RoleDTO;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;

public interface RoleService {

	DatabaseTable<RoleDTO> getTable(DatabaseParameter databaseParameter);

	RoleDTO getRole(UUID id);

	RoleDTO getUpdate(RoleDTO roleDTO);

	void getDelete(UUID id);

}
