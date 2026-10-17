package rs.formuvia.model.utils;

import java.net.HttpURLConnection;
import java.sql.Connection;

import org.modelmapper.ModelMapper;

import jakarta.servlet.http.HttpServletRequest;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.model.dto.ModelTriggerDTO;
import rs.formuvia.model.entity.Model;
import rs.formuvia.model.entity.ModelTrigger;
import rs.formuvia.model.enums.ModelType;
import rs.formuvia.utils.StringUtils;

public class UpdateModelTrigger implements ExecuteQuery<ModelTriggerDTO> {
	private final HttpServletRequest httpServletRequest;
	private final ModelTriggerDTO modelTriggerDTO;

	private CommonService commonService;
	private DatabaseService databaseService = new DatabaseServiceImpl();

	private ModelMapper modelMapper = new ModelMapper();

	private final String FUNCTION_TYPE = "trigger";
	private final String QUERY_FILE = "createOrReplaceTrigger.sql";
	public static final String NAME_TO_REPLACE = "#NAME#";
	private final String WHEN_TO_REPLACE = "#WHEN#";
	private final String ACTION_TO_REPLACE = "#ACTION#";
	public static final String TABLE_NAME_TO_REPLACE = "#TABLE_NAME#";
	private final String CONDITION_TO_REPLACE = "#CONDITION#";
	private final String CONDITION_WHEN_TEXT_TO_REPLACE = "WHEN ( #CONDITION# )";
	private final String FUNCTION_NAME_TO_REPLACE = "#FUNCTION_NAME#";

	public UpdateModelTrigger(HttpServletRequest httpServletRequest, ModelTriggerDTO modelTriggerDTO) {
		this.httpServletRequest = httpServletRequest;
		this.modelTriggerDTO = modelTriggerDTO;
		this.commonService = new CommonServiceImpl(this.httpServletRequest);
	}

	@Override
	public ModelTriggerDTO execute(Connection connection) throws Exception {

		ModelTrigger modelTrigger = StringUtils.isNull(this.modelTriggerDTO.getId()) ? new ModelTrigger()
				: this.databaseService.findById(this.modelTriggerDTO.getId(), ModelTrigger.class, connection);

		if (!UpdateColumnModel.COLUMN_NAME.matcher(modelTriggerDTO.getName()).matches()) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "invalidTriggerName",
					modelTriggerDTO.getName());
		}
		Model model = this.databaseService.findById(this.modelTriggerDTO.getModelId(), Model.class, connection);

		if (model.getType().equals(ModelType.MENU)) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "frontModel", model.getId());
		}

		if (StringUtils.notNull(this.modelTriggerDTO.getId())) {
			if (!modelTrigger.getName().equals(this.modelTriggerDTO.getName())) {
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "notSameName",
						this.modelTriggerDTO.getName());
			}
		}

		UpdateColumnModel.checkFunctionExists(FUNCTION_TYPE, this.modelTriggerDTO.getFunctionName(), connection);

		this.modelMapper.map(this.modelTriggerDTO, modelTrigger);
		modelTrigger = this.databaseService.save(modelTrigger, connection);

		String query = this.commonService.readQueryFromFile(QUERY_FILE);
		query = query.replace(NAME_TO_REPLACE, this.modelTriggerDTO.getName());
		query = query.replace(WHEN_TO_REPLACE, this.modelTriggerDTO.getTriggerWhen().name());
		query = query.replace(ACTION_TO_REPLACE, this.modelTriggerDTO.getAction().action);
		query = query.replace(TABLE_NAME_TO_REPLACE, model.getCode());

		if (!StringUtils.hasText(this.modelTriggerDTO.getCondition()))
			query = query.replace(CONDITION_TO_REPLACE, "");
		else
			query = query.replace(CONDITION_TO_REPLACE, CONDITION_WHEN_TEXT_TO_REPLACE).replace(CONDITION_TO_REPLACE,
					this.modelTriggerDTO.getCondition());

		query = query.replace(FUNCTION_NAME_TO_REPLACE, this.modelTriggerDTO.getFunctionName());

		this.databaseService.executeUpdateQuery(query, null, connection);

		return this.modelMapper.map(modelTrigger, ModelTriggerDTO.class);
	}

}
