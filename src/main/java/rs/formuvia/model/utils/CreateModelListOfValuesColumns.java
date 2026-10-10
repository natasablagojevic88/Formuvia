package rs.formuvia.model.utils;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import rs.formuvia.common.dto.ComboboxDTO;
import rs.formuvia.common.service.ResourceBundleService;
import rs.formuvia.common.service.impl.ResourceBundleServiceImpl;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.model.dto.ModelColumnDTO;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

public class CreateModelListOfValuesColumns implements ExecuteQuery<LinkedHashMap<UUID, List<ComboboxDTO>>> {
	private final HttpServletRequest httpServletRequest;
	private final UUID modelId;

	private ResourceBundleService resourceBundleService;

	public CreateModelListOfValuesColumns(HttpServletRequest httpServletRequest, UUID modelId) {
		this.httpServletRequest = httpServletRequest;
		this.modelId = modelId;
		this.resourceBundleService = new ResourceBundleServiceImpl(this.httpServletRequest);
	}

	@Override
	public LinkedHashMap<UUID, List<ComboboxDTO>> execute(Connection connection) throws Exception {
		List<ModelColumnDTO> columnDTOs = StaticData.modelColumns.stream()
				.filter(a -> a.getModelId().equals(this.modelId))
				.filter(a -> (StringUtils.notNull(a.getCodebookId()) || StringUtils.notNull(a.getListOfValuesSql())))
				.collect(Collectors.toList());

		LinkedHashMap<UUID, List<ComboboxDTO>> list = new LinkedHashMap<>();

		for (ModelColumnDTO modelColumnDTO : columnDTOs) {
			if (StringUtils.notNull(modelColumnDTO.getListOfValuesSql())) {
				list.put(modelColumnDTO.getId(), CreateForm.createListOfValues(modelColumnDTO.getListOfValuesSql(),
						connection, resourceBundleService));
			}

			if (StringUtils.notNull(modelColumnDTO.getCodebookId())) {
				list.put(modelColumnDTO.getId(), StaticData.modelCodebook.get(modelColumnDTO.getCodebookId()));
			}
		}

		return list;
	}

}
