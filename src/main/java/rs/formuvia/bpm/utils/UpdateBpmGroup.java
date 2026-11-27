package rs.formuvia.bpm.utils;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;

import org.modelmapper.ModelMapper;

import rs.formuvia.administration.entity.AppUser;
import rs.formuvia.bpm.dto.BpmGroupDTO;
import rs.formuvia.bpm.entity.BpmGroup;
import rs.formuvia.bpm.entity.BpmGroupUser;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.utils.StringUtils;

public class UpdateBpmGroup implements ExecuteQuery<BpmGroupDTO> {

	private final BpmGroupDTO bpmGroupDTO;

	private DatabaseService databaseService = new DatabaseServiceImpl();

	private ModelMapper modelMapper = new ModelMapper();

	public UpdateBpmGroup(BpmGroupDTO bpmGroupDTO) {
		this.bpmGroupDTO = bpmGroupDTO;
	}

	@Override
	public BpmGroupDTO execute(Connection connection) throws Exception {
		BpmGroup groupGroup = StringUtils.isNull(bpmGroupDTO.getId()) ? new BpmGroup()
				: this.databaseService.findById(bpmGroupDTO.getId(), BpmGroup.class, connection);

		modelMapper.map(this.bpmGroupDTO, groupGroup);
		groupGroup = this.databaseService.save(groupGroup, connection);
		List<BpmGroupUser> curentUsersForGroup = this.databaseService.findAll(
				DatabaseParameter.valueOf(DatabaseFilter.valueOf("userGroup", groupGroup.getId().toString())),
				BpmGroupUser.class, connection);

		List<UUID> usersToAdd = bpmGroupDTO.getUsers().stream().filter(
				a -> !curentUsersForGroup.stream().map(b -> b.getAppUser().getId()).toList().contains(a.getId()))
				.map(a -> a.getId()).toList();
		for (UUID userId : usersToAdd) {
			BpmGroupUser bpmGroupUser = new BpmGroupUser(null, new BpmGroup(groupGroup.getId()), new AppUser(userId));
			this.databaseService.save(bpmGroupUser, connection);
		}

		List<BpmGroupUser> usersToDelete = curentUsersForGroup.stream().filter(
				a -> !bpmGroupDTO.getUsers().stream().map(b -> b.getId()).toList().contains(a.getAppUser().getId()))
				.toList();

		for (BpmGroupUser deleteGroupUser : usersToDelete)
			this.databaseService.delete(deleteGroupUser, connection);

		return this.databaseService.executeQuery(new BpmGroupItem(groupGroup.getId()), connection);
	}

}
