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
import jakarta.validation.Valid;
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
import rs.formuvia.model.dto.ModelJasperDTO;
import rs.formuvia.model.dto.ModelProcedureDTO;
import rs.formuvia.model.dto.ModelTreeDTO;
import rs.formuvia.model.dto.ModelTriggerDTO;
import rs.formuvia.model.entity.Model;
import rs.formuvia.model.entity.ModelProcedure;
import rs.formuvia.model.service.ModelService;
import rs.formuvia.model.utils.CreateModelListOfValuesColumns;
import rs.formuvia.model.utils.DeleteModel;
import rs.formuvia.model.utils.DeleteModelJasper;
import rs.formuvia.model.utils.DeleteModelTrigger;
import rs.formuvia.model.utils.UpdateModel;
import rs.formuvia.model.utils.UpdateModelJasper;
import rs.formuvia.model.utils.UpdateModelTrigger;
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
		List<ModelTriggerDTO> triggers = StaticData.modelTriggers.stream().filter(a -> a.getModelId().equals(id))
				.sorted(Comparator.comparing(ModelTriggerDTO::getName)).collect(Collectors.toList());
		modelModelExtendedDTO.setTriggers(triggers);

		List<ModelJasperDTO> jaspers = StaticData.modelJaspers.stream().filter(a -> a.getModelId().equals(id))
				.sorted(Comparator.comparing(ModelJasperDTO::getName)).collect(Collectors.toList());
		modelModelExtendedDTO.setJaspers(jaspers);
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

	@Override
	public ModelTriggerDTO getUpdateModelTrigger(@Valid ModelTriggerDTO modelTriggerDTO) {
		UpdateModelTrigger updateModelTrigger = new UpdateModelTrigger(httpServletRequest, modelTriggerDTO);
		return this.databaseService.executeQuery(updateModelTrigger);
	}

	@Override
	public void getDeleteModelTrigger(UUID id) {
		DeleteModelTrigger deleteModelTrigger = new DeleteModelTrigger(id);
		this.databaseService.executeQuery(deleteModelTrigger);
	}

	@Override
	public ModelJasperDTO getUpdateModelJasper(ModelJasperDTO modelTriggerDTO) {
		UpdateModelJasper updateModelJasper = new UpdateModelJasper(httpServletRequest, modelTriggerDTO);
		return this.databaseService.executeQuery(updateModelJasper);
	}

	@Override
	public void getDeleteModelJasper(UUID id) {
		DeleteModelJasper deleteModelJasper = new DeleteModelJasper(id);
		this.databaseService.executeQuery(deleteModelJasper);
	}

}
