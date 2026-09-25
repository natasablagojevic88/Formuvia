package rs.formuvia.model.utils;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.model.dto.ModelColumnDTO;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.entity.ModelColumn;

@RequiredArgsConstructor
public class DeleteModelColumn implements ExecuteQuery<Void> {

	private final UUID id;

	private DatabaseService databaseService = new DatabaseServiceImpl();
	private final String DELETE_QUERY = "ALTER TABLE #table_name# DROP COLUMN #column_name#";
	private final String TABLE_NAME_TO_REPLACE = "#table_name#";
	private final String COLUMN_NAME_TO_REPLACE = "#column_name#";

	@Override
	public Void execute(Connection connection) throws Exception {
		ModelColumn modelColumn = this.databaseService.findById(id, ModelColumn.class, connection);
		databaseService.delete(modelColumn, connection);

		String query = DELETE_QUERY;
		query = query.replaceAll(TABLE_NAME_TO_REPLACE, modelColumn.getModel().getCode());
		query = query.replaceAll(COLUMN_NAME_TO_REPLACE, modelColumn.getCode());

		this.databaseService.executeUpdateQuery(query, null, connection);

		if (modelColumn.getInDescriptionForCodebook()) {
			final ModelDTO codebookModel = databaseService.findById(modelColumn.getModel().getId(), ModelDTO.class,
					connection);
			final List<ModelColumnDTO> listColumn = UpdateColumnModel.findColumnList(modelColumn.getModel().getId(),
					connection);
			connection.commit();
			new Thread(() -> {
				UpdateColumnModel.loadModelStaticList(codebookModel, listColumn, null);
			}).start();
		}

		return null;
	}

}
