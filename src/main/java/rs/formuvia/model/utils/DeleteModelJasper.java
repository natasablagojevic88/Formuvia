package rs.formuvia.model.utils;

import java.sql.Connection;
import java.util.UUID;

import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.model.entity.ModelJasper;

public class DeleteModelJasper implements ExecuteQuery<Void> {
	private UUID[] ids;

	private DatabaseService databaseService = new DatabaseServiceImpl();

	public DeleteModelJasper(UUID... ids) {
		this.ids = ids;
	}

	@Override
	public Void execute(Connection connection) throws Exception {
		for (UUID id : ids) {
			ModelJasper modelJasper = this.databaseService.findById(id, ModelJasper.class, connection);

			this.databaseService.delete(modelJasper, connection);
			this.databaseService.delete(modelJasper.getFile(), connection);
		}

		return null;
	}

}
