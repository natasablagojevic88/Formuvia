package rs.formuvia.model.utils;

import java.sql.Connection;
import java.util.UUID;

import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.model.entity.ModelTrigger;

public class DeleteModelTrigger implements ExecuteQuery<Void> {

	private final UUID id;

	private DatabaseService databaseService = new DatabaseServiceImpl();
	private CommonService commonService = new CommonServiceImpl();

	private final String QUERY_FILE = "deleteTrigger.sql";

	public DeleteModelTrigger(UUID id) {
		this.id = id;
	}

	@Override
	public Void execute(Connection connection) throws Exception {
		ModelTrigger modelTrigger = this.databaseService.findById(id, ModelTrigger.class, connection);
		this.databaseService.delete(modelTrigger, connection);
		String query = this.commonService.readQueryFromFile(QUERY_FILE);
		query = query.replace(UpdateModelTrigger.NAME_TO_REPLACE, modelTrigger.getName());
		query = query.replace(UpdateModelTrigger.TABLE_NAME_TO_REPLACE, modelTrigger.getModel().getCode());
		this.databaseService.executeUpdateQuery(query, null, connection);
		return null;
	}

}
