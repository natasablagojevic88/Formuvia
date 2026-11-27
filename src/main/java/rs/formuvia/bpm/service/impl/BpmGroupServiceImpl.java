package rs.formuvia.bpm.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jvnet.hk2.annotations.Service;
import org.modelmapper.ModelMapper;

import jakarta.inject.Inject;
import rs.formuvia.administration.dto.AppUserDTO;
import rs.formuvia.bpm.dto.BpmGroupDTO;
import rs.formuvia.bpm.dto.BpmGroupUserDTO;
import rs.formuvia.bpm.entity.BpmGroup;
import rs.formuvia.bpm.service.BpmGroupService;
import rs.formuvia.bpm.utils.BpmGroupItem;
import rs.formuvia.bpm.utils.UpdateBpmGroup;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.utils.StaticData;

@Service
public class BpmGroupServiceImpl implements BpmGroupService {

	@Inject
	private DatabaseService databaseService;

	private ModelMapper modelMapper = new ModelMapper();

	@Override
	public DatabaseTable<BpmGroupDTO> getTable(DatabaseParameter databaseParameter) {
		DatabaseTable<BpmGroupDTO> datatable = databaseService.createTable(databaseParameter, BpmGroupDTO.class);
		datatable.getList().forEach(a -> {
			a.setUsers(usersForGroup(a.getId()));
		});
		return datatable;
	}

	private List<AppUserDTO> usersForGroup(UUID groupId) {
		List<BpmGroupUserDTO> usersForGroup = this.databaseService.findAll(
				DatabaseParameter.valueOf(DatabaseFilter.valueOf("userGroupId", groupId.toString())),
				BpmGroupUserDTO.class);
		return usersForGroup.stream().map(a -> new AppUserDTO(a.getAppUserId())).toList();
	}

	@Override
	public BpmGroupDTO getBpmGroup(UUID id) {
		BpmGroupItem item = new BpmGroupItem(id);
		return this.databaseService.executeQuery(item);
	}

	@Override
	public BpmGroupDTO getUpdate(BpmGroupDTO bpmGroupDTO) {
		UpdateBpmGroup updateBpmGroup = new UpdateBpmGroup(bpmGroupDTO);
		return this.databaseService.executeQuery(updateBpmGroup);
	}

	@Override
	public void getDelete(UUID id) {
		BpmGroup group = this.databaseService.findById(id, BpmGroup.class);
		this.databaseService.delete(group);
	}

	@Override
	public List<AppUserDTO> getAllUsers() {

		return StaticData.appUsers.stream().map(a -> modelMapper.map(a, AppUserDTO.class)).map(a -> {
			a.setPassword(null);
			return a;
		}).collect(Collectors.toList());
	}

}
