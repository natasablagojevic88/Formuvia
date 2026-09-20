package rs.formuvia.common.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import rs.formuvia.common.service.ExportTableService;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.ErrorDetail;

@Path("")
@Tag(name = "Export", description = "Exporting table data to Excel. The client first reads the table it wants to export through the table endpoint of that module and then sends the result here, so the file contains exactly the rows and columns the user sees.")
public class ExportTableController {

	@Inject
	private ExportTableService exportTableService;

	@POST
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.exportTable)
	@Operation(
			summary = "Export table to Excel",
			description = "Builds an Excel file (.xlsx) from a table that was already read through a table endpoint. The table title becomes the first row, the column descriptions the second, and the rows follow in the order they were sent; numbers, dates and boolean values are written in the cell format of their column type, with boolean values translated to the language of the request. The client decides what ends up in the file: it usually asks for the table without paging, so that the export holds every row matching the current filters and sort order, not just the visible page. The file name is returned in the Content-Disposition header, which is also exposed to the browser for cross-origin requests. Requires a valid session.",
			requestBody = @RequestBody(
					description = "Table to export, exactly as returned by the table endpoint (title, columns and rows)",
					required = true,
					content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseTable.class))),
			responses = {
					@ApiResponse(responseCode = "200", description = "Excel file, with the file name in the Content-Disposition header",
							content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
									schema = @Schema(type = "string", format = "binary"))),
					@ApiResponse(responseCode = "400", description = "Table cannot be exported, for example when a column or value is not in the expected format",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
					@ApiResponse(responseCode = "401", description = "No valid session, the access token cookie is removed",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getExportTable(
			DatabaseTable<?> databaseTable
			) {
		return exportTableService.getExport(databaseTable);
	}
}
