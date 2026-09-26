package rs.formuvia.model.service.impl;

import java.util.List;
import java.util.UUID;

import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.model.dto.ModelColumnPreviewDTO;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.service.ModelPreviewService;
import rs.formuvia.model.utils.CreateForm;
import rs.formuvia.model.utils.CreateModelTable;
import rs.formuvia.utils.StaticData;

@Service
public class ModelPreviewServiceImpl implements ModelPreviewService {

	@Inject
	private DatabaseService databaseService;

	@Context
	private HttpServletRequest httpServletRequest;

	public static ModelDTO findModel(UUID modelId) {
		return StaticData.models.stream().filter(a -> a.getId().equals(modelId)).findFirst().get();
	}

	@Override
	public DatabaseTable<?> getTable(DatabaseParameter databaseParameter, UUID modelId, UUID parentId) {
		CreateModelTable createModelTable = new CreateModelTable(databaseParameter, httpServletRequest, modelId,
				parentId);

		return databaseService.executeQuery(createModelTable);
	}

	@Override
	public List<ModelColumnPreviewDTO> getForm(UUID modelId, UUID id, UUID parent) {
		CreateForm createForm = new CreateForm(httpServletRequest, modelId, id, parent);
		return this.databaseService.executeQuery(createForm);
	}

}
