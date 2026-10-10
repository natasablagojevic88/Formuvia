package rs.formuvia.model.utils;

import java.sql.Connection;
import java.util.UUID;

import org.modelmapper.ModelMapper;

import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.model.dto.ModelColumnConditionDTO;
import rs.formuvia.model.dto.ModelColumnExtendedDTO;
import rs.formuvia.model.entity.ModelColumn;

public class CreateModelColumnExtended implements ExecuteQuery<ModelColumnExtendedDTO> {
	private final UUID id;

	private DatabaseService databaseService = new DatabaseServiceImpl();
	private static ModelMapper modelMapper = new ModelMapper();

	public CreateModelColumnExtended(UUID id) {
		this.id = id;
	}

	@Override
	public ModelColumnExtendedDTO execute(Connection connection) throws Exception {
		ModelColumnExtendedDTO columnExtendedDTO = new ModelColumnExtendedDTO();
		ModelColumn modelColumn = databaseService.findById(id, ModelColumn.class, connection);
		modelMapper.map(modelColumn, columnExtendedDTO);

		DatabaseParameter databaseParameter = DatabaseParameter
				.valueOf(DatabaseFilter.valueOf("modelColumnId", id.toString()));
		columnExtendedDTO.setConditions(
				this.databaseService.findAll(databaseParameter, ModelColumnConditionDTO.class, connection));

		return columnExtendedDTO;
	}

}
