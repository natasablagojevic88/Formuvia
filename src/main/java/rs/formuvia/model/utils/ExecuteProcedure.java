package rs.formuvia.model.utils;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.model.entity.ModelProcedure;

public class ExecuteProcedure implements ExecuteQuery<LinkedHashMap<String, Object>> {
	private final HttpServletRequest httpServletRequest;
	private final UUID procedureId;
	private final UUID id;
	private CommonService commonService;
	private DatabaseService databaseService = new DatabaseServiceImpl();

	private final String PROCEDURE_NAME_TO_REPLACE = "#procedure_name#";
	private final String QUERY = "call #procedure_name#(?)";

	public ExecuteProcedure(HttpServletRequest httpServletRequest, UUID procedureId, UUID id) {
		this.httpServletRequest = httpServletRequest;
		this.procedureId = procedureId;
		this.id = id;
		this.commonService = new CommonServiceImpl(this.httpServletRequest);
	}

	@Override
	public LinkedHashMap<String, Object> execute(Connection connection) throws Exception {
		ModelProcedure modelProcedure = this.databaseService.findById(procedureId, ModelProcedure.class, connection);
		this.commonService.checkRole(modelProcedure.getModel().getUpdateRole().getCode());
		Map<Integer, Object> parameters = Map.of(1, this.id);
		this.databaseService.executeUpdateQuery(QUERY.replace(PROCEDURE_NAME_TO_REPLACE, modelProcedure.getProcedure()),
				parameters, connection);
		ObjectRow objectRow = new ObjectRow(httpServletRequest, id, modelProcedure.getModel().getId());
		return this.databaseService.executeQuery(objectRow, connection);
	}

}
