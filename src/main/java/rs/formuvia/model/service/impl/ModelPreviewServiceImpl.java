package rs.formuvia.model.service.impl;

import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import rs.formuvia.common.dto.HistoryDTO;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.dto.ModelFileVersionDTO;
import rs.formuvia.model.dto.ObjectFormDTO;
import rs.formuvia.model.service.ModelPreviewService;
import rs.formuvia.model.utils.CreateColumnChange;
import rs.formuvia.model.utils.CreateForm;
import rs.formuvia.model.utils.CreateModelHistory;
import rs.formuvia.model.utils.CreateModelTable;
import rs.formuvia.model.utils.CreateTableTotal;
import rs.formuvia.model.utils.DeleteFileVersion;
import rs.formuvia.model.utils.DeleteObject;
import rs.formuvia.model.utils.DownloadFileVersion;
import rs.formuvia.model.utils.DownloadModelFile;
import rs.formuvia.model.utils.ExecuteProcedure;
import rs.formuvia.model.utils.ListModelFileVersion;
import rs.formuvia.model.utils.ModelTemplateDownload;
import rs.formuvia.model.utils.ModelTemplateUpload;
import rs.formuvia.model.utils.ObjectRow;
import rs.formuvia.model.utils.PrintModelJasper;
import rs.formuvia.model.utils.UpdateObject;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

@Service
public class ModelPreviewServiceImpl implements ModelPreviewService {

	@Inject
	private DatabaseService databaseService;

	@Context
	private HttpServletRequest httpServletRequest;

	@Inject
	private CommonService commonService;

	public static ModelDTO findModel(UUID modelId) {
		return StaticData.models.stream().filter(a -> a.getId().equals(modelId)).findFirst()
				.orElseThrow(() -> new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noDataFound", modelId));
	}

	@Override
	public DatabaseTable<?> getTable(DatabaseParameter databaseParameter, UUID modelId, UUID parentId) {
		CreateModelTable createModelTable = new CreateModelTable(databaseParameter, httpServletRequest, modelId,
				parentId);

		return databaseService.executeQuery(createModelTable);
	}

	@Override
	public ObjectFormDTO getForm(UUID modelId, UUID id, UUID parent) {
		CreateForm createForm = new CreateForm(httpServletRequest, modelId, id, parent, true);
		return this.databaseService.executeQuery(createForm);
	}

	@Override
	public LinkedHashMap<String, Object> getUpdate(UUID modelId, LinkedHashMap<String, Object> object) {
		UpdateObject updateObject = new UpdateObject(this.httpServletRequest, object, modelId);
		return this.databaseService.executeQuery(updateObject);
	}

	@Override
	public void getDelete(UUID modelId, UUID id) {
		DeleteObject deleteObject = new DeleteObject(this.httpServletRequest, modelId, id);
		this.databaseService.executeQuery(deleteObject);

	}

	@Override
	public List<HistoryDTO> getHistory(UUID modelId, UUID id) {
		CreateModelHistory createModelHistory = new CreateModelHistory(httpServletRequest, modelId, id);
		return this.databaseService.executeQuery(createModelHistory);
	}

	@Override
	public LinkedHashMap<String, Object> getRow(UUID modelId, UUID id) {
		ObjectRow objectRow = new ObjectRow(httpServletRequest, id, modelId);
		return this.databaseService.executeQuery(objectRow);
	}

	@Override
	public Response getDownloadFile(UUID modelId, UUID id, String columnName) {
		DownloadModelFile downloadModelFile = new DownloadModelFile(httpServletRequest, modelId, id, columnName);
		return this.databaseService.executeQuery(downloadModelFile);
	}

	@Override
	public Response getModelTemplateDownload(UUID modelId) {
		ModelTemplateDownload modelTemplateDownload = new ModelTemplateDownload(httpServletRequest, modelId);
		return this.databaseService.executeQuery(modelTemplateDownload);
	}

	@Override
	public void getModelTemplateUpload(UUID fileUploadDTOId, UUID modelId, UUID parentId) {
		ModelTemplateUpload modelTemplateUpload = new ModelTemplateUpload(httpServletRequest, fileUploadDTOId, modelId,
				parentId);
		this.databaseService.executeQuery(modelTemplateUpload);

	}

	@Override
	public DatabaseTable<ModelFileVersionDTO> getListFileVersion(UUID modelId, UUID id, String columnName) {
		ListModelFileVersion listModelFileVersion = new ListModelFileVersion(httpServletRequest, modelId, id,
				columnName);
		return this.databaseService.executeQuery(listModelFileVersion);
	}

	@Override
	public Response getDownloadFileVersion(UUID modelId, UUID id, String columnName, UUID versionId) {
		DownloadFileVersion downloadFileVersion = new DownloadFileVersion(httpServletRequest, modelId, id, columnName,
				versionId);
		return this.databaseService.executeQuery(downloadFileVersion);
	}

	@Override
	public void getDeleteFileVersion(UUID modelId, UUID id, String columnName, UUID versionId) {
		DeleteFileVersion deleteFileVersion = new DeleteFileVersion(httpServletRequest, modelId, id, columnName,
				versionId);
		this.databaseService.executeQuery(deleteFileVersion);

	}

	@Override
	public LinkedHashMap<String, BigDecimal> getTableTotal(DatabaseParameter databaseParameter, UUID modelId,
			UUID parentId) {
		CreateTableTotal createTableTotal = new CreateTableTotal(httpServletRequest, databaseParameter, modelId,
				parentId);
		return this.databaseService.executeQuery(createTableTotal);
	}

	@Override
	public LinkedHashMap<String, Object> getColumnChange(LinkedHashMap<String, Object> object, UUID modelId,
			String columnName) {
		CreateColumnChange createColumnChange = new CreateColumnChange(httpServletRequest, object, modelId, columnName);
		return this.databaseService.executeQuery(createColumnChange);
	}

	@Override
	public LinkedHashMap<String, Object> getExecuteProcedure(UUID procedureId, UUID id) {
		ExecuteProcedure executeProcedure = new ExecuteProcedure(httpServletRequest, procedureId, id);
		return this.databaseService.executeQuery(executeProcedure);
	}

	@Override
	public Response getPrintModelJasper(UUID modelJasperId, UUID id) {
		PrintModelJasper printModelJasper = new PrintModelJasper(this.httpServletRequest, modelJasperId, id);
		return this.databaseService.executeQuery(printModelJasper);
	}

	@Override
	public ObjectFormDTO getFormForTable(UUID currentModelId, UUID modelId, UUID id) {
		ModelDTO modelDTO = findModel(currentModelId);
		this.commonService.checkRole(modelDTO.getPreviewRoleCode());
		if (StaticData.modelColumns.stream().filter(a -> a.getModelId().equals(currentModelId))
				.filter(a -> StringUtils.notNull(a.getCodebookId())).filter(a -> a.getCodebookId().equals(modelId))
				.count() == 0)
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noCodebookInModel", currentModelId);

		CreateForm createForm = new CreateForm(httpServletRequest, modelId, id, null, false);
		return this.databaseService.executeQuery(createForm);
	}

}
