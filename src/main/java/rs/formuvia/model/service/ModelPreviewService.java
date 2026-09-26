package rs.formuvia.model.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import rs.formuvia.common.dto.HistoryDTO;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.model.dto.ModelColumnPreviewDTO;

public interface ModelPreviewService {

	DatabaseTable<?> getTable(DatabaseParameter databaseParameter, UUID modelId, UUID parentId);

	List<ModelColumnPreviewDTO> getForm(UUID modelId, UUID id, UUID parent);

	LinkedHashMap<String, Object> getUpdate(UUID modelId, LinkedHashMap<String, Object> object);

	void getDelete(UUID modelId, UUID id);

	List<HistoryDTO> getHistory(UUID modelId, UUID id);

}
