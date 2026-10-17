package rs.formuvia.model.service.impl;

import java.net.HttpURLConnection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jvnet.hk2.annotations.Service;
import org.modelmapper.ModelMapper;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import rs.formuvia.common.dto.ComboboxDTO;
import rs.formuvia.common.service.FunctionListService;
import rs.formuvia.common.service.ResourceBundleService;
import rs.formuvia.database.enums.SearchOperation;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.exceptions.UniqueException;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.dto.ModelExtendedDTO;
import rs.formuvia.model.dto.ModelProcedureDTO;
import rs.formuvia.model.dto.ModelTreeDTO;
import rs.formuvia.model.entity.Model;
import rs.formuvia.model.entity.ModelProcedure;
import rs.formuvia.model.service.ModelService;
import rs.formuvia.model.utils.CreateModelListOfValuesColumns;
import rs.formuvia.model.utils.DeleteModel;
import rs.formuvia.model.utils.UpdateModel;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

@Service
public class ModelServiceImpl implements ModelService {

	@Inject
	private DatabaseService databaseService;

	@Inject
	private ResourceBundleService resourceBundleService;

	@Context
	private HttpServletRequest httpServletRequest;

	@Inject
	private FunctionListService functionListService;

	private final String PROCEDURE_TYPE = "PROCEDURE";

	private final String rootName = "model.root";

	private ModelMapper modelMapper = new ModelMapper();

	@Override
	public ModelExtendedDTO getModel(UUID id) {
		Model model = this.databaseService.findById(id, Model.class);
		ModelExtendedDTO modelModelExtendedDTO = modelMapper.map(model, ModelExtendedDTO.class);
		List<ModelProcedureDTO> procedures = StaticData.modelProcedures.stream().filter(a -> a.getModelId().equals(id))
				.sorted(Comparator.comparing(ModelProcedureDTO::getName)).collect(Collectors.toList());
		modelModelExtendedDTO.setProcedures(procedures);
		return modelModelExtendedDTO;
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

	@Override
	public LinkedHashMap<UUID, List<ComboboxDTO>> getModelListOfValuesColumns(UUID id) {
		CreateModelListOfValuesColumns createModelListOfValuesColumns = new CreateModelListOfValuesColumns(
				httpServletRequest, id);
		return this.databaseService.executeQuery(createModelListOfValuesColumns);
	}

	@Override
	public ModelProcedureDTO getUpdateModelProcedure(ModelProcedureDTO modelProcedureDTO) {
		ModelProcedure modelProcedure = StringUtils.isNull(modelProcedureDTO.getId()) ? new ModelProcedure()
				: this.databaseService.findById(modelProcedureDTO.getId(), ModelProcedure.class);

		List<String> procedures = this.functionListService.getAllFunction(PROCEDURE_TYPE, null);

		if (!procedures.contains(modelProcedureDTO.getProcedure())) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noProcedureFound",
					modelProcedureDTO.getProcedure());
		}

		DatabaseParameter databaseParameter = new DatabaseParameter();
		databaseParameter.getFilters().add(DatabaseFilter.valueOf("name", modelProcedureDTO.getName()));
		databaseParameter.getFilters()
				.add(DatabaseFilter.valueOf("modelId", modelProcedureDTO.getModelId().toString()));

		if (StringUtils.notNull(modelProcedureDTO.getId()))
			databaseParameter.getFilters().add(
					DatabaseFilter.valueOf("id", SearchOperation.NOT_EQUALS, modelProcedureDTO.getId().toString()));

		if (this.databaseService.exists(databaseParameter, ModelProcedureDTO.class))
			throw new UniqueException(UniqueException.findFieldFromList(ModelProcedureDTO.class, "name"),
					modelProcedureDTO.getName());

		this.modelMapper.map(modelProcedureDTO, modelProcedure);
		modelProcedure = this.databaseService.save(modelProcedure);

		return this.modelMapper.map(modelProcedure, ModelProcedureDTO.class);
	}

	@Override
	public void getDeleteModelProcedure(UUID id) {
		ModelProcedure modelProcedure = this.databaseService.findById(id, ModelProcedure.class);
		this.databaseService.delete(modelProcedure);

	}

}
