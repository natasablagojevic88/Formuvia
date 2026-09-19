package rs.formuvia.administration.controller;

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
import rs.formuvia.administration.service.AppUserService;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.ErrorDetail;
import rs.formuvia.utils.RoleList;

@Path("")
@Tag(name = "Users", description = "Administration of application users: paged listing, retrieval, creation, update and deletion. All endpoints require the ADMIN role. Passwords are never returned.")
public class AppUserController {

	@Inject
	private AppUserService appUserService;

	@POST
	@Path(ApiRoute.appuserTable)
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(
			summary = "List users",
			description = "Returns one page of users matching the given filters and sort order, together with the total number of matching users and the number of pages. The list field contains users in the AppUserDTO format.",
			requestBody = @RequestBody(
					description = "Page index, page size, filters and sort order",
					required = true,
					content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseParameter.class))),
			responses = {
					@ApiResponse(responseCode = "200", description = "Page of users",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseTable.class))),
					@ApiResponse(responseCode = "400", description = "Invalid filter or sort definition",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
					@ApiResponse(responseCode = "401", description = "No valid session",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
					@ApiResponse(responseCode = "403", description = "Current user does not have the ADMIN role",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getTable(DatabaseParameter databaseParameter) {
		return Response.ok(appUserService.getTable(databaseParameter)).build();
	}
	
	@GET
	@Path(ApiRoute.appuserId)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(
			summary = "Get user",
			description = "Returns a single user by identifier.",
			responses = {
					@ApiResponse(responseCode = "200", description = "User found",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AppUserDTO.class))),
					@ApiResponse(responseCode = "400", description = "User with the given identifier does not exist",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
					@ApiResponse(responseCode = "401", description = "No valid session",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
					@ApiResponse(responseCode = "403", description = "Current user does not have the ADMIN role",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getAppUser(@Parameter(description = "User identifier", required = true) @PathParam("id") UUID id) {
		return Response.ok(appUserService.getAppUserDTO(id)).build();
	}
	
	@POST
	@Path(ApiRoute.appuser)
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(
			summary = "Create or update user",
			description = "Creates a new user when id is empty, otherwise updates the existing user. The password is required when creating a user. When updating, the password is optional and the existing password is kept if it is omitted. The password is stored as a bcrypt hash.",
			requestBody = @RequestBody(
					description = "User data",
					required = true,
					content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AppUserDTO.class))),
			responses = {
					@ApiResponse(responseCode = "200", description = "User saved",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AppUserDTO.class))),
					@ApiResponse(responseCode = "400", description = "Password is missing when creating a user, a required field is missing, or the user to update does not exist",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
					@ApiResponse(responseCode = "401", description = "No valid session",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
					@ApiResponse(responseCode = "403", description = "Current user does not have the ADMIN role",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getUpdate(AppUserDTO appUserDTO) {
		return Response.ok(appUserService.getUpdate(appUserDTO)).build();
	}
	
	@DELETE
	@Path(ApiRoute.appuserId)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(
			summary = "Delete user",
			description = "Deletes the user with the given identifier.",
			responses = {
					@ApiResponse(responseCode = "204", description = "User deleted"),
					@ApiResponse(responseCode = "400", description = "User with the given identifier does not exist or cannot be deleted",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
					@ApiResponse(responseCode = "401", description = "No valid session",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
					@ApiResponse(responseCode = "403", description = "Current user does not have the ADMIN role",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getDelete(@Parameter(description = "User identifier", required = true) @PathParam("id") UUID id) {
		this.appUserService.getDelete(id);
		return Response.noContent().build();
	}
}
