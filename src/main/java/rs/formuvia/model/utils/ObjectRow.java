package rs.formuvia.model.utils;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.ResourceBundleService;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.common.service.impl.ResourceBundleServiceImpl;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.service.impl.ModelPreviewServiceImpl;

public class ObjectRow implements ExecuteQuery<LinkedHashMap<String, Object>> {
	private final HttpServletRequest httpServletRequest;
	private final UUID id;
	private final UUID modelId;

	private ResourceBundleService resourceBundleService;
	private CommonService commonService;

	public ObjectRow(HttpServletRequest httpServletRequest, UUID id, UUID modelId) {
		this.httpServletRequest = httpServletRequest;
		this.id = id;
		this.modelId = modelId;
		this.resourceBundleService = new ResourceBundleServiceImpl(this.httpServletRequest);
		this.commonService = new CommonServiceImpl(this.httpServletRequest);
	}

	@Override
	public LinkedHashMap<String, Object> execute(Connection connection) throws Exception {
		ModelDTO modelDTO = ModelPreviewServiceImpl.findModel(modelId);
		this.commonService.checkRole(modelDTO.getPreviewRoleCode());
		return CreateForm.findObjectById(id, modelDTO, connection, resourceBundleService);
	}

}
