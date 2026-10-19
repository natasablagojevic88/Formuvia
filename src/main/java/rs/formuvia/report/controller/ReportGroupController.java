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
import rs.formuvia.report.dto.ReportGroupDTO;
import rs.formuvia.report.service.ReportGroupService;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.ErrorDetail;
import rs.formuvia.utils.RoleList;

@Path("")
@Tag(name = "Report groups", description = "Groups the reports are sorted into: paged table, retrieval, creation, update and deletion. A group carries the role that may read the reports in it. All endpoints require a valid session and the admin role.")
public class ReportGroupController {

	@Inject
	private ReportGroupService reportGroupService;

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.reportGroupTable)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "List report groups", description = "Returns one page of report groups for display in a table. Request: pageIndex (zero-based), pageSize (all rows if omitted), filters (field = ReportGroupDTO field name, searchOperation, value in field1, upper bound in field2 for BETWEEN) and orders (fieldName, direction ASC or DESC); without orders the groups are sorted by name. Response: name (translated table title), column (columns to show, the identifier left out), allColumns (every ReportGroupDTO field with its translated label), list (groups), total and numberOfPages. Labels are translated to the language from the X-Language header.", requestBody = @RequestBody(description = "Page, page size, filters and sort order", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseParameter.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Page of report groups", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseTable.class))),
			@ApiResponse(responseCode = "400", description = "Unknown field or invalid value in a filter or sort", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getReportGroupTable(DatabaseParameter databaseParameter) {
		return Response.ok(reportGroupService.createTable(databaseParameter)).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.reportGroupId)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "Get report group", description = "Returns one report group by identifier, used to fill the edit form: its name, the identifier of its role and the code of that role.", responses = {
			@ApiResponse(responseCode = "200", description = "Report group found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ReportGroupDTO.class))),
			@ApiResponse(responseCode = "400", description = "Report group with the given identifier does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getReportGroup(@Parameter(description = "Report group identifier", required = true) @PathParam("id") UUID id) {
		return Response.ok(reportGroupService.getReportGroup(id)).build();
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.reportGroup)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "Create or update report group", description = "Creates a new report group when id is empty, otherwise updates the group with that id. The name is required and must be unique among the groups; the role is required and says who may read the reports of the group, so changing it changes who gets them from that moment on.", requestBody = @RequestBody(description = "Report group data; id empty for a new group", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ReportGroupDTO.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Report group saved", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ReportGroupDTO.class))),
			@ApiResponse(responseCode = "400", description = "Name or role is missing, another group already uses that name, or the group to update does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response updateReportGroup(@Valid ReportGroupDTO reportGroupDTO) {
		return Response.ok(reportGroupService.getUpdate(reportGroupDTO)).build();
	}

	@DELETE
	@Path(ApiRoute.reportGroupId)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "Delete report group", description = "Deletes the report group with the given identifier. The response body is empty.", responses = {
			@ApiResponse(responseCode = "204", description = "Report group deleted"),
			@ApiResponse(responseCode = "400", description = "Report group with the given identifier does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getDeleteReportGroup(@Parameter(description = "Report group identifier", required = true) @PathParam("id") UUID id) {
		reportGroupService.getDelete(id);
		return Response.noContent().build();
	}

}
