package rs.formuvia.administration.controller;

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
import rs.formuvia.administration.dto.RoleDTO;
import rs.formuvia.administration.service.AppUserService;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.ErrorDetail;
import rs.formuvia.utils.RoleList;

@Path("")
@Tag(name = "Users", description = "Administration of application users: paged table, retrieval, creation, update, deletion and assigning roles. All endpoints require a valid session and the admin role. Passwords are never returned.")
public class AppUserController {

	@Inject
	private AppUserService appUserService;

	@POST
	@Path(ApiRoute.appuserTable)
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "List users", description = "Returns one page of users for display in a table. Request: pageIndex (zero-based), pageSize (all rows if omitted), filters (field = AppUserDTO field name, searchOperation, value in field1, upper bound in field2 for BETWEEN) and orders (fieldName, direction ASC or DESC); without orders the users are sorted by username. Response: name (translated table title), column (columns to show, hidden fields such as id and password left out), allColumns (every AppUserDTO field with its translated label), list (users in the AppUserDTO format, password always null), total (number of matching users) and numberOfPages. Labels are translated to the language from the X-Language header.", requestBody = @RequestBody(description = "Page, page size, filters and sort order", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseParameter.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Page of users", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseTable.class))),
			@ApiResponse(responseCode = "400", description = "Unknown field or invalid value in a filter or sort", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getTable(DatabaseParameter databaseParameter) {
		return Response.ok(appUserService.getTable(databaseParameter)).build();
	}

	@GET
	@Path(ApiRoute.appuserId)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "Get user", description = "Returns one user by identifier, used to fill the edit form. The password is always null. Besides the user data the response carries userRoles (roles the user has) and allRoles (every role in the application), so the form can offer the full list with the current ones selected.", responses = {
			@ApiResponse(responseCode = "200", description = "User found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AppUserDTO.class))),
			@ApiResponse(responseCode = "400", description = "User with the given identifier does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getAppUser(@Parameter(description = "User identifier", required = true) @PathParam("id") UUID id) {
		return Response.ok(appUserService.getAppUserDTO(id)).build();
	}

	@POST
	@Path(ApiRoute.appuser)
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "Create or update user", description = "Creates a new user when id is empty, otherwise updates the user with that id. Username, first name, last name and the active flag must be set, and the username must be unique. The password is required when creating a user; when updating, an empty or omitted password keeps the existing one. The password is stored as a bcrypt hash. Roles are taken from userRoles: roles that are missing from the list are removed from the user and new ones are added, while allRoles is ignored on input. Returns the saved user without the password, with its roles and the list of all roles.", requestBody = @RequestBody(description = "User data; id empty for a new user", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AppUserDTO.class))), responses = {
			@ApiResponse(responseCode = "200", description = "User saved", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AppUserDTO.class))),
			@ApiResponse(responseCode = "400", description = "Password missing for a new user, a required field missing, username already taken, or the user to update does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getUpdate(@Valid AppUserDTO appUserDTO) {
		return Response.ok(appUserService.getUpdate(appUserDTO)).build();
	}

	@DELETE
	@Path(ApiRoute.appuserId)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "Delete user", description = "Deletes the user with the given identifier together with their role assignments. Only a user who has never changed any data can be deleted: once the user appears in the change history, the history must stay, so such a user can only be deactivated (active = false). Deletion also fails while the user still has session records; these are removed by the scheduled cleanup once the session has expired or the user has logged out. The response body is empty.", responses = {
			@ApiResponse(responseCode = "204", description = "User deleted"),
			@ApiResponse(responseCode = "400", description = "User does not exist, has a change history (deactivate instead), or still has session records", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getDelete(@Parameter(description = "User identifier", required = true) @PathParam("id") UUID id) {
		this.appUserService.getDelete(id);
		return Response.noContent().build();
	}

	@GET
	@Path(ApiRoute.appuserAllRoles)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "List roles", description = "Returns every role in the application (identifier, code and description), for filling the role picker when a new user is entered. The roles come from the data kept in memory, which is refreshed when the role table changes.", responses = {
			@ApiResponse(responseCode = "200", description = "All roles", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = RoleDTO.class)))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getAllRoles() {
		return Response.ok(appUserService.getAllRoles()).build();
	}
}
