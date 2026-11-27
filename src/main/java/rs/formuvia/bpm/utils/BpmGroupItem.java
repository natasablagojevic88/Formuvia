package rs.formuvia.bpm.utils;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;

import rs.formuvia.administration.dto.AppUserDTO;
import rs.formuvia.bpm.dto.BpmGroupDTO;
import rs.formuvia.bpm.dto.BpmGroupUserDTO;
import rs.formuvia.bpm.service.impl.BpmGroupServiceImpl;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.ExecuteQuery;

public class BpmGroupItem implements ExecuteQuery<BpmGroupDTO> {
	private final UUID id;

	private DatabaseService databaseService = new DatabaseServiceImpl();

	public BpmGroupItem(UUID id) {
		this.id = id;
	}

	@Override
	public BpmGroupDTO execute(Connection connection) throws Exception {
		BpmGroupDTO bpmGroupDTO = this.databaseService.findById(id, BpmGroupDTO.class, connection);

		List<AppUserDTO> users = new BpmGroupServiceImpl().getAllUsers();

		List<BpmGroupUserDTO> usersForGroup = this.databaseService.findAll(
				DatabaseParameter.valueOf(DatabaseFilter.valueOf("userGroupId", id.toString())), BpmGroupUserDTO.class,
				connection);

		bpmGroupDTO.setAllUsers(users);
		bpmGroupDTO.setUsers(users.stream()
				.filter(a -> usersForGroup.stream().map(b -> b.getAppUserId()).toList().contains(a.getId())).toList());
		return bpmGroupDTO;
	}

}
