package rs.formuvia.model.utils;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import jakarta.json.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import rs.formuvia.common.dto.ChangeDTO;
import rs.formuvia.common.dto.ComboboxDTO;
import rs.formuvia.common.dto.HistoryDTO;
import rs.formuvia.common.dto.TrackDTO;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.ResourceBundleService;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.common.service.impl.ResourceBundleServiceImpl;
import rs.formuvia.common.utils.CreateHistory;
import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.database.utils.DatabaseColumn;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.service.impl.ModelPreviewServiceImpl;
import rs.formuvia.utils.StringUtils;

public class CreateModelHistory implements ExecuteQuery<List<HistoryDTO>> {
	private final HttpServletRequest httpServletRequest;
	private final UUID modelId;
	private final UUID id;
	private CommonService commonService;
	private ResourceBundleService resourceBundleService;

	public CreateModelHistory(HttpServletRequest httpServletRequest, UUID modelId, UUID id) {
		this.httpServletRequest = httpServletRequest;
		this.modelId = modelId;
		this.id = id;
		this.commonService = new CommonServiceImpl(this.httpServletRequest);
		this.resourceBundleService = new ResourceBundleServiceImpl(this.httpServletRequest);
	}

	@Override
	public List<HistoryDTO> execute(Connection connection) throws Exception {
		List<HistoryDTO> list = new ArrayList<>();
		ModelDTO modelDTO = ModelPreviewServiceImpl.findModel(modelId);
		this.commonService.checkRole(modelDTO.getPreviewRoleCode());
		String tableName = modelDTO.getCode();
		List<TrackDTO> tracks = CreateHistory.createListTrackDTOs(tableName, id, connection);
		List<DatabaseColumn> columns = CreateModelTable.getColumnsForModel(modelDTO, resourceBundleService);
		for (TrackDTO track : tracks) {
			HistoryDTO historyDTO = CreateHistory.createHistoryDTO(track, resourceBundleService);

			LinkedHashMap<String, Object> oldData = createObjectFromJson(modelDTO, track.getOldData(), columns);
			LinkedHashMap<String, Object> newData = createObjectFromJson(modelDTO, track.getNewData(), columns);

			for (DatabaseColumn column : columns) {
				ChangeDTO changeDTO = CreateHistory.createChangeDTO(oldData.get(column.getFieldName()),
						newData.get(column.getFieldName()), this.resourceBundleService, column.getFieldName(),
						column.getColumnType());

				if (StringUtils.notNull(changeDTO)) {
					if (!column.getListOfValues().isEmpty()) {
						if (StringUtils.notNull(changeDTO.getOldData())) {
							String value = changeDTO.getOldData().toString();
							ComboboxDTO comboboxDTO = findComboboxFromList(column.getListOfValues(), value);
							if (comboboxDTO != null) {
								changeDTO.setColumnType(ColumnType.STRING);
								changeDTO.setOldData(comboboxDTO.getOption());
							}
						}

						if (StringUtils.notNull(changeDTO.getNewData())) {
							String value = changeDTO.getNewData().toString();
							ComboboxDTO comboboxDTO = findComboboxFromList(column.getListOfValues(), value);
							if (comboboxDTO != null) {
								changeDTO.setColumnType(ColumnType.STRING);
								changeDTO.setNewData(comboboxDTO.getOption());
							}
						}

					}

					historyDTO.getChanges().add(changeDTO);
				}

			}

			list.add(historyDTO);
		}

		return list;
	}

	private ComboboxDTO findComboboxFromList(List<ComboboxDTO> list, String value) {
		return list.stream().filter(a -> a.getValue().toString().equals(value)).findFirst().orElse(null);
	}

	private LinkedHashMap<String, Object> createObjectFromJson(ModelDTO modelDTO, String jsonText,
			List<DatabaseColumn> columns) {
		LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<>();
		if (!StringUtils.hasText(jsonText)) {
			return linkedHashMap;
		}

		JsonObject jsonObject = CreateHistory.createJsonObject(jsonText);

		for (DatabaseColumn databaseColumn : columns) {
			if (StringUtils.isNull(jsonObject.get(databaseColumn.getFieldName()))) {
				continue;
			}

			Object value = CreateHistory.getObjectFromJson(jsonObject, databaseColumn.getFieldName());

			if (StringUtils.isNull(value)) {
				continue;
			}

			linkedHashMap.put(databaseColumn.getFieldName(),
					CreateHistory.convertObjectByType(databaseColumn.getColumnType(), value));

		}

		return linkedHashMap;
	}

}
