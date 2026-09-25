package rs.formuvia.model.service.impl;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jvnet.hk2.annotations.Service;
import org.modelmapper.ModelMapper;

import jakarta.inject.Inject;
import rs.formuvia.common.service.ResourceBundleService;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.dto.ModelTreeDTO;
import rs.formuvia.model.entity.Model;
import rs.formuvia.model.service.ModelService;
import rs.formuvia.model.utils.UpdateModel;
import rs.formuvia.model.utils.DeleteModel;
import rs.formuvia.utils.StringUtils;

@Service
public class ModelServiceImpl implements ModelService {

	@Inject
	private DatabaseService databaseService;

	@Inject
	private ResourceBundleService resourceBundleService;

	private final String rootName = "model.root";

	private ModelMapper modelMapper = new ModelMapper();

	@Override
	public ModelDTO getModel(UUID id) {
		Model model = this.databaseService.findById(id, Model.class);
		return modelMapper.map(model, ModelDTO.class);
	}

	@Override
	public ModelDTO getUpdate(ModelDTO modelDTO) {
		UpdateModel createModel = new UpdateModel(modelDTO);
		return databaseService.executeQuery(createModel);
	}

	@Override
	public void getDelete(UUID id) {
		DeleteModel deleteModel = new DeleteModel(id);
		this.databaseService.executeQuery(deleteModel);

	}

	@Override
	public ModelTreeDTO getTree() {
		ModelTreeDTO modelTreeDTO = new ModelTreeDTO();
		modelTreeDTO.setName(this.resourceBundleService.getText(rootName));

		List<ModelDTO> listModel = this.databaseService.findAll(null, ModelDTO.class);

		List<ModelDTO> listWithoutParent = listModel.stream().filter(a -> StringUtils.isNull(a.getParentId()))
				.sorted(Comparator.comparing(ModelDTO::getName)).collect(Collectors.toList());

		for (ModelDTO model : listWithoutParent) {
			createModelTreeDTO(listModel, model, modelTreeDTO);
		}

		return modelTreeDTO;
	}

	private void createModelTreeDTO(List<ModelDTO> listModel, ModelDTO modelDTO, ModelTreeDTO parent) {
		ModelTreeDTO modelTreeDTO = modelMapper.map(modelDTO, ModelTreeDTO.class);

		List<ModelDTO> findChildren = listModel.stream().filter(a -> StringUtils.notNull(a.getParentId()))
				.filter(a -> a.getParentId().equals(modelDTO.getId())).sorted(Comparator.comparing(ModelDTO::getName))
				.collect(Collectors.toList());
		for (ModelDTO child : findChildren) {
			createModelTreeDTO(listModel, child, modelTreeDTO);
		}

		parent.getChildren().add(modelTreeDTO);
	}

}
