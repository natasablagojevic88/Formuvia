package rs.formuvia.report.controller;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.quartz.dto.JobItemDTO;
import rs.formuvia.report.dto.ReportDTO;
import rs.formuvia.report.dto.ReportParameterDTO;
import rs.formuvia.report.service.ReportService;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.ErrorDetail;
import rs.formuvia.utils.RoleList;

@Path("")
@Tag(name = "Reports", description = "Reports of one report group with their parameters and the jobs that run them by the schedule. A report is either data from a query, an action over the data, or a Jasper document. All endpoints require a valid session and the admin role.")
public class ReportController {

	@Inject
	private ReportService reportService;

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.reportTable)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "List reports of a group", description = "Returns one page of the reports that belong to the given report group; the group from the path is added to the filters, so a report of another group never comes back. Request: pageIndex (zero-based), pageSize (all rows if omitted), filters (field = ReportDTO field name, searchOperation, value in field1, upper bound in field2 for BETWEEN) and orders (fieldName, direction ASC or DESC); without orders the reports are sorted by name. Response: name (translated table title), column (columns to show, the identifier and the query left out), allColumns (every ReportDTO field with its translated label), list (reports), total and numberOfPages. Labels and the values of the type are translated to the language from the X-Language header.", requestBody = @RequestBody(description = "Page, page size, filters and sort order", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseParameter.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Page of reports of the group", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseTable.class))),
			@ApiResponse(responseCode = "400", description = "Unknown field or invalid value in a filter or sort", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getTable(DatabaseParameter databaseParameter,
			@Parameter(description = "Identifier of the report group whose reports are listed", required = true) @PathParam("reportGroupId") UUID reportGroupId) {
		return Response.ok(reportService.getTable(databaseParameter, reportGroupId)).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.reportId)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "Get report", description = "Returns one report by identifier, used to fill the edit form: its group, type, name, description and, for a report made of a query or of an action, the query itself.", responses = {
			@ApiResponse(responseCode = "200", description = "Report found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ReportDTO.class))),
			@ApiResponse(responseCode = "400", description = "Report with the given identifier does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getReport(@Parameter(description = "Report identifier", required = true) @PathParam("id") UUID id) {
		return Response.ok(reportService.getReport(id)).build();
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.report)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "Save report", description = "Inserts a report when the identifier is empty, otherwise changes the report with that identifier. The group, the type and the name are required; the name must not repeat inside the same group. For a report made of a query or of an action the query is required and is checked: it must be valid SQL, it must be a single query, and it must be a SELECT for a report of data, or CALL, UPDATE, INSERT or DELETE for a report that works over the data. For a Jasper report the query is not kept. Returns the saved report as it stands in the database.", requestBody = @RequestBody(description = "Report to save; empty identifier inserts a new one", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ReportDTO.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Saved report", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ReportDTO.class))),
			@ApiResponse(responseCode = "400", description = "Required field missing, name already taken inside the group, or the query is not allowed here", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getUpdate(ReportDTO reportDTO) {
		return Response.ok(reportService.updateReport(reportDTO)).build();
	}

	@DELETE
	@Path(ApiRoute.reportId)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "Delete report", description = "Deletes the report with the given identifier together with its parameters. Returns no content.", responses = {
			@ApiResponse(responseCode = "204", description = "Report deleted"),
			@ApiResponse(responseCode = "400", description = "Report with the given identifier does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getDelete(@Parameter(description = "Report identifier", required = true) @PathParam("id") UUID id) {
		this.reportService.deleteReport(id);
		return Response.noContent().build();
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.reportParameterTable)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "List parameters of a report", description = "Returns one page of the parameters of the given report; the report from the path is added to the filters, so a parameter of another report never comes back. Request and response have the same shape as the table of reports; the values of the data type are translated to the language from the X-Language header.", requestBody = @RequestBody(description = "Page, page size, filters and sort order", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseParameter.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Page of parameters of the report", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseTable.class))),
			@ApiResponse(responseCode = "400", description = "Unknown field or invalid value in a filter or sort", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getParameterTable(DatabaseParameter databaseParameter,
			@Parameter(description = "Identifier of the report whose parameters are listed", required = true) @PathParam("reportId") UUID reportId) {
		return Response.ok(reportService.getParameterTable(databaseParameter, reportId)).build();
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.reportParameter)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "Save parameter of a report", description = "Inserts a parameter when the identifier is empty, otherwise changes the parameter with that identifier. The report, the order, the data type, the code, the name and whether the value may be left out are required. For a report made of a query or of an action the code must stand in the query of the report as :code. A parameter cannot be of the kind file. The code and the order must not repeat inside the same report. Returns the saved parameter.", requestBody = @RequestBody(description = "Parameter to save; empty identifier inserts a new one", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ReportParameterDTO.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Saved parameter", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ReportParameterDTO.class))),
			@ApiResponse(responseCode = "400", description = "Required field missing, code not found in the query, kind not allowed, or code or order already taken inside the report", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getParameterUpdate(ReportParameterDTO reportParameterDTO) {
		return Response.ok(reportService.getParameterUpdate(reportParameterDTO)).build();
	}

	@DELETE
	@Path(ApiRoute.reportParameterId)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "Delete parameter of a report", description = "Deletes the parameter with the given identifier. Returns no content.", responses = {
			@ApiResponse(responseCode = "204", description = "Parameter deleted"),
			@ApiResponse(responseCode = "400", description = "Parameter with the given identifier does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getParameterDelete(
			@Parameter(description = "Parameter identifier", required = true) @PathParam("id") UUID id) {
		reportService.getParameterDelete(id);
		return Response.noContent().build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.reportParameterOrderNumber)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "Next order of a parameter", description = "Returns the order to offer for a new parameter of the given report: one more than the largest order already entered, or 1 when the report has no parameter yet.", responses = {
			@ApiResponse(responseCode = "200", description = "Order to offer for a new parameter", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Integer.class))),
			@ApiResponse(responseCode = "400", description = "Report with the given identifier does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getParameterOrderNumber(
			@Parameter(description = "Identifier of the report the parameter is added to", required = true) @PathParam("reportId") UUID reportId) {
		return Response.ok(reportService.getParameterOrderNumber(reportId)).build();
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.reportJobTable)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "List jobs of a report", description = "Returns one page of the jobs of the given report; the report from the path is added to the filters, so a job of another report or of a table never comes back. Request: pageIndex (zero-based), pageSize, filters and orders over JobItemDTO fields. Response: name (translated table title), column, allColumns (every JobItemDTO field with its translated label), list, total and numberOfPages; the values of the job type are translated to the language from the X-Language header.", requestBody = @RequestBody(description = "Page, page size, filters and sort order", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseParameter.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Page of jobs of the report", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseTable.class))),
			@ApiResponse(responseCode = "400", description = "Unknown field or invalid value in a filter or sort", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getJobTable(DatabaseParameter databaseParameter,
			@Parameter(description = "Identifier of the report whose jobs are listed", required = true) @PathParam("reportId") UUID reportId) {
		return Response.ok(reportService.getJobTable(databaseParameter, reportId, null)).build();
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.reportJob)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "Save job of a report", description = "Inserts a job when the identifier is empty, otherwise changes the job with that identifier. The job runs the report by the schedule. reportId is required and modelId is ignored. The job type depends on the report: a query or a Jasper report is exported to a file (EXPORT_FILE, with fileName and filePath) or sent by mail (SEND_MAIL, with receive, jobOutputType, mailSubject and smtpId; an attachment also needs fileName and, for a query, exportType); an action is only run (EXECUTE). A Jasper report needs jobOutputType and cannot be sent inline. CSV needs csvDelimiter. conditional is kept only for a query: the job then does its work only when the query returns rows. The timetable is given per unit of time (minutes, hours, day of the month, month, day of the week, year): a unit is any value (jobCronAny...), every n-th value (jobCronEvery...) or values separated by commas (jobCronIn...), never a step and a list together; a day of the month and a day of the week cannot both be chosen. The name must not repeat and cannot be changed once the job is saved. An active job is put into the schedule at once, an inactive one is taken out of it. Returns the saved job.", requestBody = @RequestBody(description = "Job to save; empty identifier inserts a new one", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JobItemDTO.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Saved job", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JobItemDTO.class))),
			@ApiResponse(responseCode = "400", description = "Required field missing, job type or output type not allowed for the report, name already taken or changed, or a timetable that cannot be read", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getJobUpdate(@Valid JobItemDTO jobItemDTO) {
		return Response.ok(reportService.getJobUpdate(jobItemDTO, true, false)).build();
	}

	@DELETE
	@Path(ApiRoute.reportJobId)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "Delete job", description = "Deletes the job with the given identifier and takes it out of the schedule, so it does not run again. It is used for the jobs of a report and for the import jobs of a table alike. Returns no content.", responses = {
			@ApiResponse(responseCode = "204", description = "Job deleted"),
			@ApiResponse(responseCode = "400", description = "Job with the given identifier does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getJobDelete(
			@Parameter(description = "Job identifier", required = true) @PathParam("id") UUID id) {
		reportService.getJobDelete(id);
		return Response.noContent().build();
	}

}
