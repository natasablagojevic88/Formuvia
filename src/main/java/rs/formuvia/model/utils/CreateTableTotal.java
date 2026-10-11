package rs.formuvia.model.utils;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.ResourceBundleService;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.common.service.impl.ResourceBundleServiceImpl;
import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.database.utils.QueryTableInfo;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.service.impl.ModelPreviewServiceImpl;
import rs.formuvia.utils.StringUtils;

public class CreateTableTotal implements ExecuteQuery<LinkedHashMap<String, BigDecimal>> {
	private final HttpServletRequest httpServletRequest;
	private final DatabaseParameter databaseParameter;
	private final UUID modelId;
	private final UUID parentId;

	private ResourceBundleService resourceBundleService;
	private CommonService commonService;

	public CreateTableTotal(HttpServletRequest httpServletRequest, DatabaseParameter databaseParameter, UUID modelId,
			UUID parentId) {
		this.httpServletRequest = httpServletRequest;
		this.databaseParameter = databaseParameter;
		this.modelId = modelId;
		this.parentId = parentId;
		this.resourceBundleService = new ResourceBundleServiceImpl(this.httpServletRequest);
		this.commonService = new CommonServiceImpl(this.httpServletRequest);
	}

	@Override
	public LinkedHashMap<String, BigDecimal> execute(Connection connection) throws Exception {
		ModelDTO modelDTO = ModelPreviewServiceImpl.findModel(modelId);
		commonService.checkRole(modelDTO.getPreviewRoleCode());
		if (StringUtils.notNull(this.parentId)) {
			this.databaseParameter.getFilters().add(
					DatabaseFilter.valueOf(UpdateModel.PARENT_COLUMN_NAME, this.parentId.toString(), ColumnType.UUID));
		}
		DatabaseTable<LinkedHashMap<String, Object>> databaseTable = new DatabaseTable<>();
		QueryTableInfo queryTableInfo = CreateModelTable.createQueryTableInfo(modelDTO, databaseTable,
				resourceBundleService);
		CreateModelTable.calculateTotal(databaseTable, queryTableInfo, databaseParameter, connection);
		return databaseTable.getTotalColumns();
	}

}
