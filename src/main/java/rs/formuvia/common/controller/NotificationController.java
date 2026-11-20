package rs.formuvia.common.controller;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import rs.formuvia.common.dto.NotificationDTO;
import rs.formuvia.common.service.NotificationService;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.ErrorDetail;

@Path("")
@Tag(name = "Notifications", description = "Notifications of the current user: how many are unread, the unread ones for the bell in the header, all of them as a table, and marking one as read or unread. All endpoints require a valid session and work only with the notifications of the user who is logged in; no role is needed.")
public class NotificationController {

	@Inject
	private NotificationService notificationService;

	@GET
	@Path(ApiRoute.notificationCount)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Count unread notifications", description = "Returns how many notifications of the current user are not read yet, as a bare number (for example 3). The front asks for it when the application is entered and after a notification is marked, and shows it on the bell in the header.", responses = {
			@ApiResponse(responseCode = "200", description = "Number of unread notifications", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Long.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getCount() {
		return Response.ok(notificationService.getCount()).build();
	}

	@GET
	@Path(ApiRoute.notificationListUnseen)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "List unread notifications", description = "Returns every unread notification of the current user, all at once and without paging, newest first. title and body are translated by the X-Language header when they are keys of the resource bundle, and come back as they were written otherwise. url, when it is set, is either a path inside the application (opened from the root of the front) or a full http/https address.", responses = {
			@ApiResponse(responseCode = "200", description = "Unread notifications", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = NotificationDTO.class)))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getListUnseen() {
		return Response.ok(notificationService.getListUnseen()).build();
	}

	@POST
	@Path(ApiRoute.notificationListAll)
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(summary = "List all notifications", description = "Returns one page of all notifications of the current user, read and unread, unread first and then newest first. Request: pageIndex (zero-based), pageSize (all rows if omitted), filters (field = NotificationDTO field name, searchOperation, value in field1, upper bound in field2 for BETWEEN) and orders (fieldName, direction ASC or DESC); the filter by the user is added by the server. Response: name, column (columns to show), allColumns (every NotificationDTO field with its translated label), list (notifications in the NotificationDTO format, title and body translated as in the unread list), total and numberOfPages.", requestBody = @RequestBody(description = "Page, page size, filters and sort order", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseParameter.class))), responses = {
			@ApiResponse(responseCode = "200", description = "One page of notifications", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseTable.class))),
			@ApiResponse(responseCode = "400", description = "Unknown field in a filter or in the sort order", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response notificationListAll(DatabaseParameter databaseParameter) {
		return Response.ok(notificationService.notificationListAll(databaseParameter)).build();
	}

	@POST
	@Path(ApiRoute.notificationSeen)
	@Operation(summary = "Mark notification as read", description = "Marks one notification of the current user as read. The front calls it when the notification is opened; the number of unread notifications goes down by one. No request body.", responses = {
			@ApiResponse(responseCode = "204", description = "Marked as read"),
			@ApiResponse(responseCode = "400", description = "Notification with the given identifier does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "The notification belongs to another user", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getNotificationSeen(
			@Parameter(description = "Notification identifier", required = true) @PathParam("id") UUID id) {
		notificationService.getNotificationChange(id, true);
		return Response.noContent().build();
	}

	@POST
	@Path(ApiRoute.notificationUnSeen)
	@Operation(summary = "Mark notification as unread", description = "Marks one notification of the current user as unread again, so it comes back under the bell. No request body.", responses = {
			@ApiResponse(responseCode = "204", description = "Marked as unread"),
			@ApiResponse(responseCode = "400", description = "Notification with the given identifier does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "The notification belongs to another user", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getNotificationUnSeen(
			@Parameter(description = "Notification identifier", required = true) @PathParam("id") UUID id) {
		notificationService.getNotificationChange(id, false);
		return Response.noContent().build();
	}
}
