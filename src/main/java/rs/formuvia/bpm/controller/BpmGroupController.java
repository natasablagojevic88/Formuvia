package rs.formuvia.bpm.controller;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import rs.formuvia.administration.dto.AppUserDTO;
import rs.formuvia.bpm.dto.BpmGroupDTO;
import rs.formuvia.bpm.service.BpmGroupService;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.ErrorDetail;
import rs.formuvia.utils.RoleList;

@Tag(name = "Process user groups", description = "Groups of users for the steps of processes: paged table, retrieval, creation, update and deletion, with the users that belong to each group. All endpoints require a valid session and the admin role.")
@Path("")
public class BpmGroupController {

	@Inject
	private BpmGroupService bpmGroupService;

	@RolesAllowed(RoleList.ADMIN)
	@POST
	@Path(ApiRoute.bpmGroupTable)
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(summary = "List process user groups", description = "Returns one page of the groups for display in a table. Request: pageIndex (zero-based), pageSize (all rows if omitted), filters (field = BpmGroupDTO field name, searchOperation, value in field1, upper bound in field2 for BETWEEN) and orders (fieldName, direction ASC or DESC); without orders the groups are sorted by name. Response: name (translated table title), column (columns to show), allColumns (every BpmGroupDTO field with its translated label), list (groups without their users), total and numberOfPages.", requestBody = @RequestBody(description = "Page, page size, filters and sort order", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseParameter.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Page of groups", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseTable.class))),
			@ApiResponse(responseCode = "400", description = "Unknown field or invalid value in a filter or sort", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getTable(DatabaseParameter databaseParameter) {
		return Response.ok(bpmGroupService.getTable(databaseParameter)).build();
	}

	@RolesAllowed(RoleList.ADMIN)
	@GET
	@Path(ApiRoute.bpmGroupId)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Get process user group", description = "Returns one group with the users in it (users) and every user of the application (allUsers), to choose the members from. Passwords are not sent.", responses = {
			@ApiResponse(responseCode = "200", description = "Group found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = BpmGroupDTO.class))),
			@ApiResponse(responseCode = "400", description = "Group with the given identifier does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getBpmGroup(@Parameter(description = "Group identifier", required = true) @PathParam("id") UUID id) {
		return Response.ok(bpmGroupService.getBpmGroup(id)).build();
	}

	@RolesAllowed(RoleList.ADMIN)
	@POST
	@Path(ApiRoute.bpmGroup)
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(summary = "Create or update process user group", description = "Creates a new group when id is empty, otherwise updates the group with that id. The members are set to the users sent in users (only their id is read): users not yet in the group are added and users no longer in the list are taken out. The name must be unique. allUsers is ignored.", requestBody = @RequestBody(description = "Group with its users; id empty for a new group", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = BpmGroupDTO.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Group saved; the response is the group as it was saved, with its users and every user", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = BpmGroupDTO.class))),
			@ApiResponse(responseCode = "400", description = "The name is missing or already used, or a user does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getUpdate(@Valid BpmGroupDTO bpmGroupDTO) {
		return Response.ok(bpmGroupService.getUpdate(bpmGroupDTO)).build();
	}

	@RolesAllowed(RoleList.ADMIN)
	@DELETE
	@Path(ApiRoute.bpmGroupId)
	@Operation(summary = "Delete process user group", description = "Deletes the group with the given identifier; its members are taken out of it, the users themselves stay. The response body is empty.", responses = {
			@ApiResponse(responseCode = "204", description = "Group deleted"),
			@ApiResponse(responseCode = "400", description = "Group with the given identifier does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getDelete(@Parameter(description = "Group identifier", required = true) @PathParam("id") UUID id) {
		bpmGroupService.getDelete(id);
		return Response.noContent().build();
	}

	@RolesAllowed(RoleList.ADMIN)
	@GET
	@Path(ApiRoute.bpmGroupAllUser)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "List all users for a process user group", description = "Returns every user of the application, without passwords, to choose the members of a new group from.", responses = {
			@ApiResponse(responseCode = "200", description = "Every user", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AppUserDTO.class)))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getAllUsers() {
		return Response.ok(bpmGroupService.getAllUsers()).build();
	}

}
