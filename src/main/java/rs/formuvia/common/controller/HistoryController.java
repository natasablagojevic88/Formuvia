package rs.formuvia.common.controller;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import rs.formuvia.common.dto.HistoryDTO;
import rs.formuvia.common.service.HistoryService;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.ErrorDetail;

@Path("")
@Tag(name = "History", description = "History of changes for a single record. Every change of a tracked table is written by a database trigger, together with the user that made it and the time, so the history shows what the record looked like before and after each change.")
public class HistoryController {

	@Inject
	private HistoryService historyService;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.history)
	@Operation(summary = "Get record history", description = "Returns the changes of one record, newest first. Every entry carries the action (added, changed or deleted, translated to the language from the X-Language header), the time, the user that made the change, and the list of fields that actually changed with their old and new value; field names are translated as well. The record is addressed by the name of its DTO class and its identifier. Only fields of the DTO are compared, so fields that are not shown in the application do not appear in the history either. Requires a valid session and one of the roles the DTO is restricted to.", responses = {
			@ApiResponse(responseCode = "200", description = "Changes of the record, newest first; an empty list when nothing has been changed yet", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = HistoryDTO.class)))),
			@ApiResponse(responseCode = "400", description = "Unknown class name, or the record cannot be read", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session, the access token cookie is removed", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have a role that may see this data", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getHistory(
			@Parameter(description = "Name of the DTO class of the record, as sent in className of the table response (for example AppUserDTO)", required = true) @PathParam("className") String className,
			@Parameter(description = "Record identifier", required = true) @PathParam("id") UUID id) {
		return Response.ok(historyService.getHistory(className, id)).build();
	}
}
