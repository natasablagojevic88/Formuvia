package rs.formuvia.administration.service.impl;

import java.util.UUID;

import org.jvnet.hk2.annotations.Service;
import org.modelmapper.ModelMapper;

import jakarta.inject.Inject;
import rs.formuvia.administration.dto.RoleDTO;
import rs.formuvia.administration.entity.Role;
import rs.formuvia.administration.service.RoleService;
import rs.formuvia.database.enums.SearchOperation;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.exceptions.UniqueException;

@Service
public class RoleServiceImpl implements RoleService {

	@Inject
	private DatabaseService databaseService;

	private ModelMapper modelMapper = new ModelMapper();

	@Override
	public DatabaseTable<RoleDTO> getTable(DatabaseParameter databaseParameter) {

		return databaseService.createTable(databaseParameter, RoleDTO.class);
	}

	@Override
	public RoleDTO getRole(UUID id) {
		RoleDTO role = this.databaseService.findById(id, RoleDTO.class);
		return role;
	}

	@Override
	public RoleDTO getUpdate(RoleDTO roleDTO) {
		Role role = roleDTO.getId() == null ? new Role() : this.databaseService.findById(roleDTO.getId(), Role.class);

		if (roleDTO.getId() != null) {
			if (this.databaseService.exists(
					DatabaseParameter.valueOf(new DatabaseFilter[] { DatabaseFilter.valueOf("code", roleDTO.getCode()),
							DatabaseFilter.valueOf("id", SearchOperation.NOT_EQUALS, roleDTO.getId().toString()) }),
					Role.class)) {
				throw new UniqueException(UniqueException.findFieldFromList(RoleDTO.class, "code"), roleDTO.getCode());
			}
		} else {
			if (this.databaseService.exists(DatabaseParameter
					.valueOf(new DatabaseFilter[] { DatabaseFilter.valueOf("code", roleDTO.getCode()) }), Role.class)) {
				throw new UniqueException(UniqueException.findFieldFromList(RoleDTO.class, "code"), roleDTO.getCode());
			}
		}

		modelMapper.map(roleDTO, role);

		role = this.databaseService.save(role);

		return getRole(role.getId());
	}

	@Override
	public void getDelete(UUID id) {
		Role role = this.databaseService.findById(id, Role.class);
		this.databaseService.delete(role);
	}

}
