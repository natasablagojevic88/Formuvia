package rs.formuvia.report.controller;

import java.util.LinkedHashMap;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import rs.formuvia.report.dto.ReportInfoDTO;
import rs.formuvia.report.dto.ReportResultDTO;
import rs.formuvia.report.service.ReportPreviewService;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.ErrorDetail;

@Path("")
@Tag(name = "Report / Running", description = "Running one report: what it asks for, the rows a SELECT report gives back, the same rows as an Excel file, and running a report that works over the data. Every endpoint requires a valid session and the role of the group the report belongs to; the parameters are sent as a value by parameter code and are put into the query as ?, so the query itself is never built out of them.")
public class ReportPreviewController {

	@Inject
	private ReportPreviewService reportPreviewService;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.reportPreview)
	@Operation(operationId = "getReportInfo", summary = "What a report asks for", description = "Returns everything needed to offer one report to the user: its name and description, its kind (SQL for a report of data, EXECUTE for one that works over the data, JASPER for a document), and its parameters in the order they are asked for. For every parameter: its code (the name it stands under in the query), its translated name, the kind of value it asks for, the value its default query gives back at that moment, and the records of its list of values (value that gets used and text that is shown) when it has one. Names are translated to the language from the X-Language header.", responses = {
			@ApiResponse(responseCode = "200", description = "The report and its parameters", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ReportInfoDTO.class))),
			@ApiResponse(responseCode = "400", description = "Report with the given identifier does not exist, or a query of a default or of a list of values failed", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the role of the group this report belongs to", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getReportPreviewExcel(
			@Parameter(description = "Identifier of the report that is being offered", required = true) @PathParam("reportId") UUID reportId) {
		return Response.ok(reportPreviewService.getReportInfo(reportId, true)).build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.reportPreviewTable)
	@Operation(operationId = "getReportPreviewTable", summary = "Rows of a report", description = "Runs the query of a report of the SQL kind and returns the rows it gives back, all of them at once - this answer has no paging, no filters and no sorting, because the order and the contents are decided by the query itself. Every :code in the query is replaced by ? and the value sent under that code is bound in its place, so the same parameter may stand in the query more than once. Response: columns (for every column of the query the key it stands under in a row - k1, k2, ... - its name and the kind of value it holds) and list (the rows, value by that key). A report of another kind is refused.", requestBody = @RequestBody(description = "Value by parameter code; a parameter left out is sent as null", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = "object", description = "Value by parameter code", example = "{\"datum_od\": \"2026-01-01\", \"komitent\": \"0190e0a0-0000-7000-8000-000000000000\"}"))), responses = {
			@ApiResponse(responseCode = "200", description = "Columns of the query and every row it gave back", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ReportResultDTO.class))),
			@ApiResponse(responseCode = "400", description = "Report with the given identifier does not exist, the report is not of the SQL kind, or the query itself failed", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the role of the group this report belongs to", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getReportPreviewTable(LinkedHashMap<String, Object> parameters,
			@Parameter(description = "Identifier of the report that is run", required = true) @PathParam("reportId") UUID reportId) {
		return Response.ok(reportPreviewService.getReportPreviewTable(parameters, reportId, true)).build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Path(ApiRoute.reportPreviewExcel)
	@Operation(operationId = "getReportPreviewExcel", summary = "Rows of a report as an Excel file", description = "Runs the query of a report of the SQL kind with the given parameters and returns its rows as an .xlsx file: the first row holds the names of the columns, and every value is written in the form its kind calls for, with two decimals for a decimal number. The file is named in the Content-Disposition header. A report of another kind is refused.", requestBody = @RequestBody(description = "Value by parameter code; a parameter left out is sent as null", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = "object", description = "Value by parameter code", example = "{\"datum_od\": \"2026-01-01\"}"))), responses = {
			@ApiResponse(responseCode = "200", description = "The Excel file, with its name in the Content-Disposition header", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM, schema = @Schema(type = "string", format = "binary"))),
			@ApiResponse(responseCode = "400", description = "Report with the given identifier does not exist, the report is not of the SQL kind, or the query itself failed", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the role of the group this report belongs to", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getReportPreviewExcel(LinkedHashMap<String, Object> parameters,
			@Parameter(description = "Identifier of the report that is run", required = true) @PathParam("reportId") UUID reportId) {
		return reportPreviewService.getReportPreviewExcel(parameters, reportId, true);
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.reportPreviewExecute)
	@Operation(operationId = "getReportExecute", summary = "Run a report that works over the data", description = "Runs the query of a report of the EXECUTE kind (CALL, UPDATE, INSERT or DELETE) with the given parameters. Nothing is returned - the report changes the data, it does not show them. A report of another kind is refused.", requestBody = @RequestBody(description = "Value by parameter code; a parameter left out is sent as null", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = "object", description = "Value by parameter code", example = "{\"godina\": 2026}"))), responses = {
			@ApiResponse(responseCode = "204", description = "The report has been run"),
			@ApiResponse(responseCode = "400", description = "Report with the given identifier does not exist, the report is not of the EXECUTE kind, or the query itself failed", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the role of the group this report belongs to", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getReportExecute(LinkedHashMap<String, Object> parameters,
			@Parameter(description = "Identifier of the report that is run", required = true) @PathParam("reportId") UUID reportId) {
		reportPreviewService.getReportExecute(parameters, reportId, true);
		return Response.noContent().build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Path(ApiRoute.reportPreviewJasper)
	@Operation(operationId = "getReportPreviewJasper", summary = "A report of the JASPER kind as a PDF", description = "Fills the compiled .jasper file of a report of the JASPER kind with the given parameters and returns the document it makes as a PDF. The value sent under a parameter code is turned into the kind of value that parameter asks for and handed to the report under its own name, so the names in the file and the codes of the parameters are the same thing; the report reads the data over the connection of the application itself, and the language of the document is the one from the X-Language header. The file is named in the Content-Disposition header. A report of another kind is refused, because it is not handed over as a PDF.", requestBody = @RequestBody(description = "Value by parameter code; a parameter left out is sent as null", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = "object", description = "Value by parameter code", example = "{\"datum_od\": \"2026-01-01\"}"))), responses = {
			@ApiResponse(responseCode = "200", description = "The document as a PDF, with its name in the Content-Disposition header", content = @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary"))),
			@ApiResponse(responseCode = "400", description = "Report with the given identifier does not exist, the report is not of the JASPER kind, the file of the report is missing, or the report itself failed", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the role of the group this report belongs to", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getReportPreviewJasper(LinkedHashMap<String, Object> parameters,
			@Parameter(description = "Identifier of the report that is run", required = true) @PathParam("reportId") UUID reportId) {
		return reportPreviewService.getReportPreviewJasper(parameters, reportId, true);
	}
}
