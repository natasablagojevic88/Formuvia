package rs.formuvia.model.utils;

import java.net.HttpURLConnection;
import java.sql.Connection;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.model.entity.Model;
import rs.formuvia.model.enums.ModelType;

@RequiredArgsConstructor
public class DeleteModel implements ExecuteQuery<Void> {
	private final UUID id;

	private DatabaseService databaseService = new DatabaseServiceImpl();
	private final String DROP_TABLE_PREFIX_QUERY = "DROP TABLE ";

	@Override
	public Void execute(Connection connection) throws Exception {
		Model model = this.databaseService.findById(id, Model.class, connection);

		if (this.databaseService.exists(DatabaseParameter.valueOf(DatabaseFilter.valueOf("parent", id.toString())),
				Model.class, connection)) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "modelHasChildren", null);
		}

		this.databaseService.delete(model, connection);

		if (model.getType().equals(ModelType.TABLE)) {
			String query = DROP_TABLE_PREFIX_QUERY + model.getCode();
			this.databaseService.executeUpdateQuery(query, null, connection);
		}

		return null;
	}

}
