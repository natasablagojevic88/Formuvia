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
import rs.formuvia.bpm.dto.BpmGroupDTO;
import rs.formuvia.bpm.dto.BpmModelDTO;
import rs.formuvia.bpm.service.BpmModelService;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.ErrorDetail;
import rs.formuvia.utils.RoleList;

@Tag(name = "Processes", description = "Processes (BPM) of a model: paged table, retrieval, creation, update and deletion. A process is a tree of steps: a task a user accepts or rejects, a database procedure that is executed and a notification sent to users; every step may have an entry condition in SQL and may end the process. All endpoints require a valid session and the admin role.")
@Path("")
public class BpmModelController {

	@Inject
	private BpmModelService bpmModelService;

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.bpmModelList)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "List processes of a model", description = "Returns one page of the processes of the given model for display in a table. Request: pageIndex (zero-based), pageSize (all rows if omitted), filters (field = BpmModelDTO field name, searchOperation, value in field1, upper bound in field2 for BETWEEN) and orders (fieldName, direction ASC or DESC); without orders the processes are sorted by name. Response: name (translated table title), column (columns to show), allColumns (every BpmModelDTO field with its translated label), list (processes without their steps), total and numberOfPages.", requestBody = @RequestBody(description = "Page, page size, filters and sort order", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseParameter.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Page of processes", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseTable.class))),
			@ApiResponse(responseCode = "400", description = "Unknown field or invalid value in a filter or sort", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getTable(DatabaseParameter databaseParameter,
			@Parameter(description = "Identifier of the model the processes belong to", required = true) @PathParam("modelId") UUID modelId) {
		return Response.ok(bpmModelService.getTable(databaseParameter, modelId)).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.bpmModelId)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "Get process", description = "Returns one process with all its steps, used to fill the process designer: the name, the mail server (smtpId), the start and end procedure and items, the steps that start the process. Every step carries its children (the steps after it; previousAccept says whether a child follows the acceptance or the rejection of a task), the user or the group who decides a task (appUserId or bpmGroupId), the users who get its notification (receiveNotification) and a group that gets it as well (bpmGroupId) and the columns a task may change (changeColumns).", responses = {
			@ApiResponse(responseCode = "200", description = "Process found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = BpmModelDTO.class))),
			@ApiResponse(responseCode = "400", description = "Process with the given identifier does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getBpmModel(
			@Parameter(description = "Process identifier", required = true) @PathParam("id") UUID id) {
		return Response.ok(bpmModelService.getBpmModel(id)).build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.bpmModel)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "Create or update process", description = "Creates a new process when id is empty, otherwise updates the process with that id, together with the whole tree of steps sent in items; steps that are no longer in the tree are deleted. The name must be unique within the model, a mail server (smtpId) the notifications go through is required, and the start and end procedure, when given, must exist in the database. Every step needs a name and a type: a task needs the user or the group who decides (appUserId or bpmGroupId), a procedure step an existing procedure, a notification a subject, a text and at least one recipient or a group. An entry condition must be a SELECT. At least one step must end the process, and a task must have a next step or the end of the process both after acceptance and after rejection; any other step a next step or the end.", requestBody = @RequestBody(description = "Process with its tree of steps; id empty for a new process", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = BpmModelDTO.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Process saved; the response is the process as it was saved, with the identifiers of new steps", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = BpmModelDTO.class))),
			@ApiResponse(responseCode = "400", description = "A required field is missing, the name is already used in the model, a procedure does not exist, a condition is not a SELECT, or a step leaves the process without a next step or an end", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getUpdate(@Valid BpmModelDTO bpmModelDTO) {
		return Response.ok(bpmModelService.getUpdate(bpmModelDTO)).build();
	}

	@DELETE
	@Path(ApiRoute.bpmModelId)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "Delete process", description = "Deletes the process with the given identifier together with all its steps. The response body is empty.", responses = {
			@ApiResponse(responseCode = "204", description = "Process deleted"),
			@ApiResponse(responseCode = "400", description = "Process with the given identifier does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getDelete(
			@Parameter(description = "Process identifier", required = true) @PathParam("id") UUID id) {
		bpmModelService.getDelete(id);
		return Response.noContent().build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.bpmModelAllGroups)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "List all process user groups", description = "Returns every process user group (id and name; users and allUsers are empty), to give a task or a notification of a process to a group.", responses = {
			@ApiResponse(responseCode = "200", description = "Every group", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = BpmGroupDTO.class)))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getAllGroups() {
		return Response.ok(bpmModelService.getAllGroups()).build();
	}
}
