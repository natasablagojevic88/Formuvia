package rs.formuvia.model.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import rs.formuvia.common.dto.ComboboxDTO;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.dto.ModelTreeDTO;

public interface ModelService {

	ModelDTO getUpdate(ModelDTO modelDTO);

	ModelDTO getModel(UUID id);

	void getDelete(UUID id);

	ModelTreeDTO getTree();

	LinkedHashMap<UUID, List<ComboboxDTO>> getModelListOfValuesColumns(UUID id);

}
