package rs.formuvia.bpm.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Inject;
import rs.formuvia.bpm.dto.BpmGroupDTO;
import rs.formuvia.bpm.dto.BpmModelDTO;
import rs.formuvia.bpm.entity.BpmModel;
import rs.formuvia.bpm.service.BpmModelService;
import rs.formuvia.bpm.utils.CreateBpmModelDTO;
import rs.formuvia.bpm.utils.UpdateBpmModel;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.utils.StaticData;

@Service
public class BpmModelServiceImpl implements BpmModelService {

	@Inject
	private DatabaseService databaseService;

	@Override
	public DatabaseTable<BpmModelDTO> getTable(DatabaseParameter databaseParameter, UUID modelId) {
		databaseParameter.getFilters().add(DatabaseFilter.valueOf("modelId", modelId.toString()));

		return databaseService.createTable(databaseParameter, BpmModelDTO.class);
	}

	@Override
	public BpmModelDTO getBpmModel(UUID id) {
		return databaseService.executeQuery(new CreateBpmModelDTO(id));
	}

	@Override
	public BpmModelDTO getUpdate(BpmModelDTO bpmModelDTO) {
		UpdateBpmModel updateBpmModel = new UpdateBpmModel(bpmModelDTO);
		return this.databaseService.executeQuery(updateBpmModel);
	}

	@Override
	public void getDelete(UUID id) {
		BpmModel bpmModel = this.databaseService.findById(id, BpmModel.class);
		this.databaseService.delete(bpmModel);

	}

	@Override
	public List<BpmGroupDTO> getAllGroups() {

		return StaticData.bpmGroups.stream().collect(Collectors.toList());
	}

}
