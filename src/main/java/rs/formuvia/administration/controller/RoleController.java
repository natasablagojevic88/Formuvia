package rs.formuvia.administration.controller;

import java.util.UUID;

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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import rs.formuvia.administration.dto.RoleDTO;
import rs.formuvia.administration.service.RoleService;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.ErrorDetail;
import rs.formuvia.utils.RoleList;

@Path("")
@Tag(name = "Roles", description = "Administration of roles: paged table, retrieval, creation, update and deletion. Roles are given to users through the Users endpoints; a role is what @RolesAllowed on an endpoint checks. All endpoints require a valid session and the admin role.")
public class RoleController {

	@Inject
	private RoleService roleService;

	@Path(ApiRoute.roleTable)
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RolesAllowed(RoleList.ADMIN)
	@POST
	@Operation(summary = "List roles", description = "Returns one page of roles for display in a table. Request: pageIndex (zero-based), pageSize (all rows if omitted), filters (field = RoleDTO field name, searchOperation, value in field1, upper bound in field2 for BETWEEN) and orders (fieldName, direction ASC or DESC); without orders the roles are sorted by code. Response: name (translated table title), column (columns to show, the identifier left out), allColumns (every RoleDTO field with its translated label), list (roles), total and numberOfPages. Labels are translated to the language from the X-Language header.", requestBody = @RequestBody(description = "Page, page size, filters and sort order", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseParameter.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Page of roles", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseTable.class))),
			@ApiResponse(responseCode = "400", description = "Unknown field or invalid value in a filter or sort", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getTable(DatabaseParameter databaseParameter) {
		return Response.ok(roleService.getTable(databaseParameter)).build();
	}

	@Path(ApiRoute.roleId)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed(RoleList.ADMIN)
	@GET
	@Operation(summary = "Get role", description = "Returns one role by identifier, used to fill the edit form.", responses = {
			@ApiResponse(responseCode = "200", description = "Role found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RoleDTO.class))),
			@ApiResponse(responseCode = "400", description = "Role with the given identifier does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getRole(@Parameter(description = "Role identifier", required = true) @PathParam("id") UUID id) {
		return Response.ok(roleService.getRole(id)).build();
	}

	@Path(ApiRoute.role)
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RolesAllowed(RoleList.ADMIN)
	@POST
	@Operation(summary = "Create or update role", description = "Creates a new role when id is empty, otherwise updates the role with that id. The code is required and must be unique; the description is optional. Changing the code changes what @RolesAllowed on the endpoints matches, so an endpoint can become unreachable for users who had that role.", requestBody = @RequestBody(description = "Role data; id empty for a new role", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RoleDTO.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Role saved", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RoleDTO.class))),
			@ApiResponse(responseCode = "400", description = "Code is missing, another role already uses that code, or the role to update does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getUpdate(@Valid RoleDTO roleDTO) {
		return Response.ok(roleService.getUpdate(roleDTO)).build();
	}

	@Path(ApiRoute.roleId)
	@RolesAllowed(RoleList.ADMIN)
	@DELETE
	@Operation(summary = "Delete role", description = "Deletes the role with the given identifier. The role is removed from every user that had it, so those users lose the access that the role granted. The response body is empty.", responses = {
			@ApiResponse(responseCode = "204", description = "Role deleted"),
			@ApiResponse(responseCode = "400", description = "Role with the given identifier does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getDelete(@Parameter(description = "Role identifier", required = true) @PathParam("id") UUID id) {
		this.roleService.getDelete(id);
		return Response.noContent().build();
	}

}
