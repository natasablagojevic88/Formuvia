package rs.formuvia.model.service.impl;

import java.util.List;
import java.util.UUID;

import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Inject;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.model.dto.ModelColumnConditionDTO;
import rs.formuvia.model.dto.ModelColumnDTO;
import rs.formuvia.model.dto.ModelColumnExtendedDTO;
import rs.formuvia.model.entity.ModelColumnCondition;
import rs.formuvia.model.service.ModelColumnService;
import rs.formuvia.model.utils.CreateModelColumnExtended;
import rs.formuvia.model.utils.DeleteModelColumn;
import rs.formuvia.model.utils.UpdateColumnModel;
import rs.formuvia.model.utils.UpdateModelColumnCondition;

@Service
public class ModelColumnServiceImpl implements ModelColumnService {

	@Inject
	private DatabaseService databaseService;

	@Override
	public List<ModelColumnDTO> getList(UUID modelId) {
		List<ModelColumnDTO> list = databaseService.findAll(
				DatabaseParameter.valueOf(DatabaseFilter.valueOf("modelId", modelId.toString())), ModelColumnDTO.class);
		return list;
	}

	@Override
	public ModelColumnExtendedDTO getUpdate(ModelColumnDTO modelColumnDTO) {
		UpdateColumnModel updateColumnModel = new UpdateColumnModel(modelColumnDTO);
		return databaseService.executeQuery(updateColumnModel);
	}

	@Override
	public ModelColumnExtendedDTO getModelColumn(UUID id) {
		CreateModelColumnExtended createModelColumnExtended = new CreateModelColumnExtended(id);
		return this.databaseService.executeQuery(createModelColumnExtended);
	}

	@Override
	public void getDelete(UUID id) {
		DeleteModelColumn deleteModelColumn = new DeleteModelColumn(id);
		this.databaseService.executeQuery(deleteModelColumn);

	}

	@Override
	public ModelColumnConditionDTO getUpdateColumnModelCondition(ModelColumnConditionDTO modelColumnConditionDTO) {
		UpdateModelColumnCondition updateModelColumnCondition = new UpdateModelColumnCondition(modelColumnConditionDTO);
		return this.databaseService.executeQuery(updateModelColumnCondition);
	}

	@Override
	public void getDeleteColumnModelCondition(UUID id) {
		ModelColumnCondition modelColumnCondition = this.databaseService.findById(id, ModelColumnCondition.class);
		this.databaseService.delete(modelColumnCondition);
	}

}
