package rs.formuvia.model.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.core.Response;
import rs.formuvia.common.dto.HistoryDTO;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.model.dto.ModelFileVersionDTO;
import rs.formuvia.model.dto.ObjectFormDTO;

public interface ModelPreviewService {

	DatabaseTable<?> getTable(DatabaseParameter databaseParameter, UUID modelId, UUID parentId);

	ObjectFormDTO getForm(UUID modelId, UUID id, UUID parent);

	LinkedHashMap<String, Object> getUpdate(UUID modelId, LinkedHashMap<String, Object> object);

	void getDelete(UUID modelId, UUID id);

	List<HistoryDTO> getHistory(UUID modelId, UUID id);

	LinkedHashMap<String, Object> getRow(UUID modelId, UUID id);

	Response getDownloadFile(UUID modelId, UUID id, String columnName);

	Response getModelTemplateDownload(UUID modelId);

	void getModelTemplateUpload(UUID fileUploadDTOId, UUID modelId, UUID parentId);

	DatabaseTable<ModelFileVersionDTO> getListFileVersion(UUID modelId, UUID id, String columnName);

	Response getDownloadFileVersion(UUID modelId, UUID id, String columnName, UUID versionId);

	void getDeleteFileVersion(UUID modelId, UUID id, String columnName, UUID versionId);

}
