package rs.formuvia.model.controller;

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
import rs.formuvia.model.dto.ModelColumnDTO;
import rs.formuvia.model.service.ModelColumnService;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.ErrorDetail;
import rs.formuvia.utils.RoleList;

@Path("")
@Tag(name = "Model columns", description = "Fields of a table defined through the model. A field is two things at once: a column of the table in the database and one input in its entry dialog, so every change here is also a change of the database schema. All endpoints require a valid session and the admin role.")
public class ModelColumnController {

	@Inject
	private ModelColumnService modelColumnService;

	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed(RoleList.ADMIN)
	@GET
	@Path(ApiRoute.modelColumnTable)
	@Operation(summary = "List fields of a model", description = "Returns every field of the given model, used to draw the form designer and to check how much of the grid is already taken. Each field carries its label and column name, data type, the number of decimal places for a decimal number, the codebook it links to, its position in the grid (rowIndex, columnIndex and colspan, all counted from one) and its options: nullable, showInTable, editable, textArea and inDescriptionForCodebook. An empty list means the model has no fields yet.", responses = {
			@ApiResponse(responseCode = "200", description = "Fields of the model", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = ModelColumnDTO.class)))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getList(
			@Parameter(description = "Identifier of the model whose fields are returned", required = true) @PathParam("modelId") UUID modelId) {
		return Response.ok(modelColumnService.getList(modelId)).build();
	}

	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed(RoleList.ADMIN)
	@GET
	@Path(ApiRoute.modelColumnId)
	@Operation(summary = "Get field", description = "Returns one field by identifier, used to fill the form when an existing field is opened in the designer.", responses = {
			@ApiResponse(responseCode = "200", description = "Field found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ModelColumnDTO.class))),
			@ApiResponse(responseCode = "400", description = "Field with the given identifier does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getModelColumn(
			@Parameter(description = "Field identifier", required = true) @PathParam("id") UUID id) {
		return Response.ok(modelColumnService.getModelColumn(id)).build();
	}

	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RolesAllowed(RoleList.ADMIN)
	@POST
	@Path(ApiRoute.modelColumn)
	@Operation(summary = "Create or update field", description = "Creates a new field when id is empty, otherwise updates the field with that id. Creating a field also adds the column to the table in the database, and a field of type UUID adds the foreign key towards the codebook it points to. Because of that, three properties are fixed once the column exists and are silently kept at their stored values when a different one is sent: the data type, the codebook and whether the field is optional. Everything else can be changed freely. The place in the grid must fit: columnIndex plus colspan may not exceed the number of columns of the entry dialog, rowIndex may not exceed its number of rows, and the place may not be taken by another field. The column name must be lowercase letters, digits and underscore starting with a letter, and both the name and the label must be unique within the model. A decimal number requires the number of decimal places; for any other type the length is set by the server. A long text is only kept for a text field, and a field of type UUID is never part of a codebook label. Both SQL queries, the default value and the list of values, must be a single SELECT written without an asterisk, returning exactly one column for a default value and exactly two, the stored value and the text shown, for a list of values.", requestBody = @RequestBody(description = "Field data; id empty for a new field", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ModelColumnDTO.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Field saved, with the values the server settled on", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ModelColumnDTO.class))),
			@ApiResponse(responseCode = "400", description = "A required field missing, the place in the grid does not fit or is taken, invalid or duplicated column name or label, codebook missing for a field of type UUID, number of decimal places missing, or an SQL query that is not a plain SELECT, uses an asterisk, or returns the wrong number of columns", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getUpdate(@Valid ModelColumnDTO modelColumnDTO) {
		return Response.ok(modelColumnService.getUpdate(modelColumnDTO)).build();
	}

	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed(RoleList.ADMIN)
	@DELETE
	@Path(ApiRoute.modelColumnId)
	@Operation(summary = "Delete field", description = "Deletes the field from the model and drops its column from the table in the database, together with every value stored in it. This cannot be undone. If the field was part of the label shown for this table in codebooks, that label is rebuilt afterwards. The response body is empty.", responses = {
			@ApiResponse(responseCode = "204", description = "Field and its column deleted"),
			@ApiResponse(responseCode = "400", description = "Field with the given identifier does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getDelete(
			@Parameter(description = "Field identifier", required = true) @PathParam("id") UUID id) {
		modelColumnService.getDelete(id);
		return Response.noContent().build();
	}
}
