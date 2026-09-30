package rs.formuvia.model.utils;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.ResourceBundleService;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.common.service.impl.ResourceBundleServiceImpl;
import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.model.dto.ModelColumnDTO;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.service.impl.ModelPreviewServiceImpl;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

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
		LinkedHashMap<String, Object> object = CreateForm.findObjectById(id, modelDTO, connection,
				resourceBundleService);
		for (ModelColumnDTO modelColumnDTO : StaticData.modelColumns.stream()
				.filter(a -> a.getModelId().equals(modelDTO.getId()))
				.filter(a -> a.getColumnType().equals(ColumnType.FILE)).collect(Collectors.toList())) {

			if (StringUtils.isNull(object.get(modelColumnDTO.getCode()))) {
				continue;
			}

			UUID fileId = UUID.fromString(object.get(modelColumnDTO.getCode()).toString());
			object.put(modelColumnDTO.getCode(), CreateForm.findModelFileDtoFromId(fileId, connection, true));
		}
		return object;
	}

}
