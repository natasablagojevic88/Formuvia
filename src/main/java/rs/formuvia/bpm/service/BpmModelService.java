package rs.formuvia.bpm.service;

import java.util.List;
import java.util.UUID;

import rs.formuvia.bpm.dto.BpmGroupDTO;
import rs.formuvia.bpm.dto.BpmModelDTO;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;

public interface BpmModelService {

	DatabaseTable<BpmModelDTO> getTable(DatabaseParameter databaseParameter, UUID modelId);

	BpmModelDTO getBpmModel(UUID id);

	BpmModelDTO getUpdate(BpmModelDTO bpmModelDTO);

	void getDelete(UUID id);

	List<BpmGroupDTO> getAllGroups();

}
