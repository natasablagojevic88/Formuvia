package rs.formuvia.model.service;

import java.util.List;
import java.util.UUID;

import rs.formuvia.model.dto.ModelColumnDTO;

public interface ModelColumnService {

	List<ModelColumnDTO> getList(UUID modelId);

	ModelColumnDTO getUpdate(ModelColumnDTO modelColumnDTO);

	ModelColumnDTO getModelColumn(UUID id);

	void getDelete(UUID id);

}
