package rs.formuvia.model.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import rs.formuvia.common.dto.ComboboxDTO;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.dto.ModelProcedureDTO;
import rs.formuvia.model.dto.ModelTreeDTO;
import rs.formuvia.model.dto.ModelTriggerDTO;

public interface ModelService {

	ModelDTO getUpdate(ModelDTO modelDTO);

	ModelDTO getModel(UUID id);

	void getDelete(UUID id);

	ModelTreeDTO getTree();

	LinkedHashMap<UUID, List<ComboboxDTO>> getModelListOfValuesColumns(UUID id);

	ModelProcedureDTO getUpdateModelProcedure(ModelProcedureDTO modelProcedureDTO);

	void getDeleteModelProcedure(UUID id);

	ModelTriggerDTO getUpdateModelTrigger(@Valid ModelTriggerDTO modelTriggerDTO);

	void getDeleteModelTrigger(UUID id);

}
