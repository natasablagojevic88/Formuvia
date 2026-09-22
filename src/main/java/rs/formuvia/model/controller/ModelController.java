package rs.formuvia.model.controller;

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
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.dto.ModelTreeDTO;
import rs.formuvia.model.service.ModelService;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.ErrorDetail;
import rs.formuvia.utils.RoleList;

@Path("")
@Tag(name = "Model", description = "Definition of the application structure as a tree: menus at the first level, tables under menus and subtables under tables. Saving a new table creates it in the database; deleting a table drops it together with its data. All endpoints require a valid session and the admin role.")
public class ModelController {

	@Inject
	private ModelService modelService;
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.modelTree)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(
			summary = "Get model tree",
			description = "Returns the whole model as one tree. The root is not a stored item: it has no identifier and its name is translated to the language from the X-Language header. Its children are the menus, their children the tables, and tables can have subtables to any depth. Items on every level are sorted by name.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Model tree",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ModelTreeDTO.class))),
					@ApiResponse(responseCode = "401", description = "No valid session",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
					@ApiResponse(responseCode = "403", description = "Current user does not have the admin role",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getTree(
			) {
		return Response.ok(modelService.getTree()).build();
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.modelId)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(
			summary = "Get model item",
			description = "Returns one menu or table by identifier, used to fill the edit form.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Model item",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ModelDTO.class))),
					@ApiResponse(responseCode = "400", description = "Item with the given identifier does not exist",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
					@ApiResponse(responseCode = "401", description = "No valid session",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
					@ApiResponse(responseCode = "403", description = "Current user does not have the admin role",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getModel(
			@Parameter(description = "Model item identifier", required = true) @PathParam("id") UUID id
			) {
		return Response.ok(modelService.getModel(id)).build();
	}
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.model)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(
			summary = "Create or update model item",
			description = "Creates a new item when id is empty, otherwise updates the item with that id. Rules: a MENU has no parent, and its code and roles are cleared; a TABLE must have a parent (a menu or another table), a code and all four roles (view, add, edit, delete). The code is the table name in the database: lowercase letters, digits and underscore, starting with a letter, at most 63 characters, unique among items. The name must be unique as well. When a new TABLE is saved, the table is created in the database with an id column; under another table it also gets a parent column with a foreign key that deletes its rows together with the parent row. Changing the code of an existing table does not rename the table in the database.",
			requestBody = @RequestBody(
					description = "Menu or table data; id empty for a new item",
					required = true,
					content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ModelDTO.class))),
			responses = {
					@ApiResponse(responseCode = "200", description = "Item saved",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ModelDTO.class))),
					@ApiResponse(responseCode = "400", description = "A rule is broken: a menu with a parent, a table without a parent, a missing or invalid code, a missing role, a name or code that already exists, or a table that already exists in the database",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
					@ApiResponse(responseCode = "401", description = "No valid session",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
					@ApiResponse(responseCode = "403", description = "Current user does not have the admin role",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getUpdate(
			@Valid ModelDTO modelDTO
			) {
		return Response.ok(modelService.getUpdate(modelDTO)).build();
	}
	
	@DELETE
	@Path(ApiRoute.modelId)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(
			summary = "Delete model item",
			description = "Deletes a menu or a table. An item that still has child items cannot be deleted; delete the children first. Deleting a TABLE also drops the table from the database together with all its data, which cannot be undone. The response body is empty.",
			responses = {
					@ApiResponse(responseCode = "204", description = "Item deleted"),
					@ApiResponse(responseCode = "400", description = "Item does not exist or still has child items",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
					@ApiResponse(responseCode = "401", description = "No valid session",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
					@ApiResponse(responseCode = "403", description = "Current user does not have the admin role",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getDelete(
			@Parameter(description = "Model item identifier", required = true) @PathParam("id") UUID id
			) {
		modelService.getDelete(id);
		return Response.noContent().build();
	}
	
}
