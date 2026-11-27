package rs.formuvia.bpm.service;

import java.util.List;
import java.util.UUID;

import rs.formuvia.administration.dto.AppUserDTO;
import rs.formuvia.bpm.dto.BpmGroupDTO;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;

public interface BpmGroupService {

	DatabaseTable<BpmGroupDTO> getTable(DatabaseParameter databaseParameter);

	BpmGroupDTO getBpmGroup(UUID id);

	BpmGroupDTO getUpdate(BpmGroupDTO bpmGroupDTO);

	void getDelete(UUID id);

	List<AppUserDTO> getAllUsers();

}
