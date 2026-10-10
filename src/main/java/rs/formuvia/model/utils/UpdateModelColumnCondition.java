package rs.formuvia.model.utils;

import java.net.HttpURLConnection;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

import org.modelmapper.ModelMapper;

import rs.formuvia.database.enums.SearchOperation;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.exceptions.NotNullException;
import rs.formuvia.exceptions.UniqueException;
import rs.formuvia.model.dto.ModelColumnConditionDTO;
import rs.formuvia.model.entity.ModelColumn;
import rs.formuvia.model.entity.ModelColumnCondition;
import rs.formuvia.utils.StringUtils;

public class UpdateModelColumnCondition implements ExecuteQuery<ModelColumnConditionDTO> {
	private final ModelColumnConditionDTO modelColumnConditionDTO;

	private ModelMapper modelMapper = new ModelMapper();
	private DatabaseService databaseService = new DatabaseServiceImpl();

	public UpdateModelColumnCondition(ModelColumnConditionDTO modelColumnConditionDTO) {
		this.modelColumnConditionDTO = modelColumnConditionDTO;
	}

	@Override
	public ModelColumnConditionDTO execute(Connection connection) throws Exception {
		ModelColumn modelConditionColumn = this.databaseService.findById(modelColumnConditionDTO.getConditionColumnId(),
				ModelColumn.class, connection);
		if (!(modelColumnConditionDTO.getSearchOperation().equals(SearchOperation.IS_NULL)
				|| modelColumnConditionDTO.getSearchOperation().equals(SearchOperation.IS_NOT_NULL))) {
			if (StringUtils.isNull(modelColumnConditionDTO.getField1()))
				throw new NotNullException(
						UniqueException.findFieldFromList(modelColumnConditionDTO.getClass(), "field1"));

			if (StringUtils.notNull(modelConditionColumn.getListOfValuesSql()))
				if (!modelColumnConditionDTO.getSearchOperation().equals(SearchOperation.EQUALS))
					throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noSearchOperationAllowed",
							modelColumnConditionDTO.getSearchOperation().name());

			List<SearchOperation> stringOperations = Arrays.asList(new SearchOperation[] { SearchOperation.CONTAINS,
					SearchOperation.ENDS_WITH, SearchOperation.STARTS_WITH });
			switch (modelConditionColumn.getColumnType()) {
			case BIGDECIMAL, INTEGER, LOCALDATE, LOCALDATETIME, LOCALTIME, LONG:
				if (stringOperations.contains(modelColumnConditionDTO.getSearchOperation()))
					throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noSearchOperationAllowed",
							modelColumnConditionDTO.getSearchOperation().name());
				break;
			case BOOLEAN, UUID:
				if (!modelColumnConditionDTO.getSearchOperation().equals(SearchOperation.EQUALS))
					throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noSearchOperationAllowed",
							modelColumnConditionDTO.getSearchOperation().name());
				break;
			case FILE:
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noConditionAllowedOnThisTable",
						modelConditionColumn.getCode());
			case STRING:
				if (!stringOperations.contains(modelColumnConditionDTO.getSearchOperation())
						&& (!modelColumnConditionDTO.getSearchOperation().equals(SearchOperation.EQUALS))
						&& (!modelColumnConditionDTO.getSearchOperation().equals(SearchOperation.NOT_EQUALS)))
					throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noSearchOperationAllowed",
							modelColumnConditionDTO.getSearchOperation().name());

				break;

			}

		} else {
			modelColumnConditionDTO.setField1(null);
			modelColumnConditionDTO.setField2(null);
		}

		if (modelColumnConditionDTO.getSearchOperation().equals(SearchOperation.BETWEEN)) {
			if (StringUtils.isNull(modelColumnConditionDTO.getField2()))
				throw new NotNullException(
						UniqueException.findFieldFromList(modelColumnConditionDTO.getClass(), "field2"));
		}

		ModelColumn modelColumn = this.databaseService.findById(modelColumnConditionDTO.getModelColumnId(),
				ModelColumn.class, connection);

		if (!modelConditionColumn.getModel().getId().equals(modelColumn.getModel().getId())) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "wrongModelForColumn", null);
		}

		ModelColumnCondition modelColumnCondition = StringUtils.isNull(modelColumnConditionDTO.getId())
				? new ModelColumnCondition()
				: this.databaseService.findById(modelColumnConditionDTO.getId(), ModelColumnCondition.class,
						connection);
		modelMapper.map(modelColumnConditionDTO, modelColumnCondition);

		modelColumnCondition = this.databaseService.save(modelColumnCondition, connection);

		return modelMapper.map(modelColumnCondition, ModelColumnConditionDTO.class);
	}

}
