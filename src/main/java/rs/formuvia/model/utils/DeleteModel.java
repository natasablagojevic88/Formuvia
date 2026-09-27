package rs.formuvia.model.utils;

import java.net.HttpURLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.RequiredArgsConstructor;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.model.entity.Model;
import rs.formuvia.model.enums.ModelType;
import rs.formuvia.utils.StaticData;

@RequiredArgsConstructor
public class DeleteModel implements ExecuteQuery<Void> {
	private final UUID id;

	private DatabaseService databaseService = new DatabaseServiceImpl();
	private final String DROP_TABLE_PREFIX_QUERY = "DROP TABLE ";
	private final String UNLINSTEN_QUERY = "UNLISTEN ";

	private Logger logger = LogManager.getLogger(getClass());

	@Override
	public Void execute(Connection connection) throws Exception {
		Model model = this.databaseService.findById(id, Model.class, connection);

		if (this.databaseService.exists(
				DatabaseParameter.valueOf(DatabaseFilter.valueOf(UpdateModel.PARENT_COLUMN_NAME, id.toString())),
				Model.class, connection)) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "modelHasChildren", null);
		}

		this.databaseService.delete(model, connection);

		if (model.getType().equals(ModelType.TABLE)) {
			String query = DROP_TABLE_PREFIX_QUERY + model.getCode();
			this.databaseService.executeUpdateQuery(query, null, connection);
			connection.commit();
			new Thread(() -> {
				try {
					PreparedStatement preparedStatement = StaticData.databaseListenConnection
							.prepareStatement(UNLINSTEN_QUERY + UpdateColumnModel.LISTEN_PREFIX + model.getCode());
					preparedStatement.execute();
					preparedStatement.close();
					StaticData.modelsToListen.removeIf(a -> a.equals(id));
					StaticData.modelCodebook.remove(id);
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}).start();
		}

		return null;
	}

}
