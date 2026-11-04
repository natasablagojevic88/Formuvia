package rs.formuvia.common.controller;

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
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import rs.formuvia.common.service.WebhookService;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.quartz.dto.JobItemWebhookDTO;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.ErrorDetail;
import rs.formuvia.utils.RoleList;

@Path("")
@Tag(name = "Webhook keys", description = "Keys with which the outside world runs one job of the schedule: the list of the keys of a job, making a key and taking one away. All endpoints here require a valid session and the admin role. The call that runs the job is in 'Webhook' and needs no session, only the key.")
public class WebhookController {

	@Inject
	private WebhookService webhookService;

	@POST
	@RolesAllowed(RoleList.ADMIN)
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.webhookList)
	@Operation(summary = "List the keys of a job", description = "Returns one page of the keys of the job with the given identifier. Request: pageIndex (zero-based), pageSize (all rows if omitted), filters (field = JobItemWebhookDTO field name, searchOperation, value in field1) and orders (fieldName, direction ASC or DESC); the filter by the job is added by the server, so a filter on jobId is not needed. Response: name (translated table title), column (columns to show), allColumns (every JobItemWebhookDTO field with its translated label), list (keys in the JobItemWebhookDTO format: bearerToken and description), total and numberOfPages. The key itself is kept as it was made and comes back whole, so it can be read again and handed over.", requestBody = @RequestBody(description = "Page, page size, filters and sort order", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseParameter.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Page of the keys of the job", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseTable.class))),
			@ApiResponse(responseCode = "400", description = "A filter or sort names an unknown field", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "The user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getWebhookTable(DatabaseParameter databaseParameter,
			@Parameter(description = "Identifier of the job whose keys are returned", required = true) @PathParam("jobId") UUID jobId) {
		return Response.ok(webhookService.getWebhookTable(databaseParameter, jobId)).build();
	}

	@POST
	@RolesAllowed(RoleList.ADMIN)
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.webhook)
	@Operation(summary = "Make a key or change its description", description = "With an empty id a new key is made for the job in jobId: the server makes it itself, the one sent in the request is ignored, and it comes back in the answer. With an id the key stays as it is and only the description is changed. The description is a note about who was given the key and may be empty.", requestBody = @RequestBody(description = "The key: jobId and description; id only when an existing key is changed", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JobItemWebhookDTO.class))), responses = {
			@ApiResponse(responseCode = "200", description = "The key as it is stored, together with the key itself", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JobItemWebhookDTO.class))),
			@ApiResponse(responseCode = "400", description = "No job with that identifier, or jobId is missing", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "The user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getUpdate(@Valid JobItemWebhookDTO itemWebhookDTO) {
		return Response.ok(webhookService.getUpdate(itemWebhookDTO)).build();
	}

	@DELETE
	@RolesAllowed(RoleList.ADMIN)
	@Path(ApiRoute.webhookId)
	@Operation(summary = "Take a key away", description = "Deletes the key, so the call that carries it stops running the job from that moment on. The job itself and its timetable stay as they are. The response body is empty.", responses = {
			@ApiResponse(responseCode = "204", description = "The key was taken away"),
			@ApiResponse(responseCode = "400", description = "No key with that identifier", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "The user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getDelete(
			@Parameter(description = "Identifier of the key that is taken away", required = true) @PathParam("id") UUID id) {
		webhookService.getDelete(id);
		return Response.noContent().build();
	}
}
