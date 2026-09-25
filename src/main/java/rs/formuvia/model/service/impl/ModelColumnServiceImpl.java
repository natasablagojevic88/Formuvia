package rs.formuvia.model.service.impl;

import java.util.List;
import java.util.UUID;

import org.jvnet.hk2.annotations.Service;
import org.modelmapper.ModelMapper;

import jakarta.inject.Inject;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.model.dto.ModelColumnDTO;
import rs.formuvia.model.entity.ModelColumn;
import rs.formuvia.model.service.ModelColumnService;
import rs.formuvia.model.utils.DeleteModelColumn;
import rs.formuvia.model.utils.UpdateColumnModel;

@Service
public class ModelColumnServiceImpl implements ModelColumnService {

	@Inject
	private DatabaseService databaseService;

	private ModelMapper modelMapper = new ModelMapper();

	@Override
	public List<ModelColumnDTO> getList(UUID modelId) {
		List<ModelColumnDTO> list = databaseService.findAll(
				DatabaseParameter.valueOf(DatabaseFilter.valueOf("modelId", modelId.toString())), ModelColumnDTO.class);
		return list;
	}

	@Override
	public ModelColumnDTO getUpdate(ModelColumnDTO modelColumnDTO) {
		UpdateColumnModel updateColumnModel = new UpdateColumnModel(modelColumnDTO);
		return databaseService.executeQuery(updateColumnModel);
	}

	@Override
	public ModelColumnDTO getModelColumn(UUID id) {
		ModelColumn modelColumn = this.databaseService.findById(id, ModelColumn.class);
		return modelMapper.map(modelColumn, ModelColumnDTO.class);
	}

	@Override
	public void getDelete(UUID id) {
		DeleteModelColumn deleteModelColumn = new DeleteModelColumn(id);
		this.databaseService.executeQuery(deleteModelColumn);

	}

}
