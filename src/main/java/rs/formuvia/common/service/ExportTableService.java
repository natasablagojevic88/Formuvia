package rs.formuvia.common.service;

import jakarta.ws.rs.core.Response;
import rs.formuvia.database.utils.DatabaseTable;

public interface ExportTableService {

	Response getExport(DatabaseTable<?> databaseTable);

}
