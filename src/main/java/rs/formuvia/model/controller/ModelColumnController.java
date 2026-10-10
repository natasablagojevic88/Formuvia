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
import rs.formuvia.model.dto.ModelColumnConditionDTO;
import rs.formuvia.model.dto.ModelColumnDTO;
import rs.formuvia.model.dto.ModelColumnExtendedDTO;
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
	@Operation(summary = "List fields of a model", description = "Returns every field of the given model, used to draw the form designer and to check how much of the grid is already taken. Each field carries its label and column name, data type, the number of decimal places for a decimal number, the codebook it links to, its position in the grid (rowIndex, columnIndex and colspan, all counted from one) its options (nullable, showInTable, showable, editable, textArea and inDescriptionForCodebook) and the sorting the table opens with (initSortOrder and initSortDirection). showInTable decides whether the field is a column of the list, showable whether it is drawn on the entry form at all. An empty list means the model has no fields yet.", responses = {
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
	@Operation(summary = "Get field", description = "Returns one field by identifier, together with the list of conditions that decide when the field is editable and when it is shown, used to fill the form when an existing field is opened in the designer. The list of fields of a model does not carry the conditions, so the designer reads a field through this endpoint before opening it.", responses = {
			@ApiResponse(responseCode = "200", description = "Field found, with its conditions", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ModelColumnExtendedDTO.class))),
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
	@Operation(summary = "Create or update field", description = "Creates a new field when id is empty, otherwise updates the field with that id. Creating a field also adds the column to the table in the database; a field of type UUID adds the foreign key towards the codebook it points to, and a field of type FILE the foreign key towards the table of stored files. Because of that, three properties are fixed once the column exists and are silently kept at their stored values when a different one is sent: the data type, the codebook and whether the field is optional. Everything else can be changed freely. The place in the grid must fit: columnIndex plus colspan may not exceed the number of columns of the entry dialog, rowIndex may not exceed its number of rows, and the place may not be taken by another field. The column name must be lowercase letters, digits and underscore starting with a letter, and both the name and the label must be unique within the model. A decimal number requires the number of decimal places; for any other type the length is set by the server. A long text is only kept for a text field, a codebook is kept only for a field of type UUID, and a field of type UUID is never part of a codebook label. A field of type FILE is only chosen and stored, so its default value query, its list of values, its long text and its part in a codebook label are all cleared. A column that holds an identifier, a link to a codebook or a file, cannot be sorted by, so its sorting order and direction are cleared as well; a field with no sorting order has no direction either. Both SQL queries, the default value and the list of values, must be a single SELECT written without an asterisk, returning exactly one column for a default value and exactly two, the stored value and the text shown, for a list of values.", requestBody = @RequestBody(description = "Field data; id empty for a new field", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ModelColumnDTO.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Field saved, with the values the server settled on and its conditions", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ModelColumnExtendedDTO.class))),
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
	public Response getDelete(@Parameter(description = "Field identifier", required = true) @PathParam("id") UUID id) {
		modelColumnService.getDelete(id);
		return Response.noContent().build();
	}

	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed(RoleList.ADMIN)
	@POST
	@Path(ApiRoute.modelColumnCondition)
	@Operation(summary = "Create or update field condition", description = "Creates a new condition when id is empty, otherwise updates the condition with that id. A condition makes one field of the entry form depend on the value of another field of the same form: type EDITABLE leaves the field editable only while the condition holds, type SHOWABLE draws the field only while it holds. A field with no condition of a kind behaves as its own options say. The condition is written the same way as a filter of a table: conditionColumnId is the field whose value is tested, searchOperation says how, and field1 and field2 carry the values compared against, always as text. Every operation needs field1, except IS_NULL and IS_NOT_NULL, which test only whether the other field is empty and keep no values at all; BETWEEN is the only operation that also needs field2. A field cannot depend on itself and both fields must belong to the same model.", requestBody = @RequestBody(description = "Condition data; id empty for a new condition", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ModelColumnConditionDTO.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Condition saved", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ModelColumnConditionDTO.class))),
			@ApiResponse(responseCode = "400", description = "A required value missing for the chosen operation, or a field or condition with the given identifier does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getUpdateColumnModelCondition(@Valid ModelColumnConditionDTO modelColumnConditionDTO) {
		return Response.ok(modelColumnService.getUpdateColumnModelCondition(modelColumnConditionDTO)).build();
	}

	@RolesAllowed(RoleList.ADMIN)
	@DELETE
	@Path(ApiRoute.modelColumnConditionId)
	@Operation(summary = "Delete field condition", description = "Deletes one condition of a field. The field itself and its values stay untouched; it simply stops depending on the other field. Deleting the last condition of a kind returns the field to the behaviour its own options describe. The response body is empty.", responses = {
			@ApiResponse(responseCode = "204", description = "Condition deleted"),
			@ApiResponse(responseCode = "400", description = "Condition with the given identifier does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getDeleteColumnModelCondition(
			@Parameter(description = "Condition identifier", required = true) @PathParam("id") UUID id) {
		modelColumnService.getDeleteColumnModelCondition(id);
		return Response.noContent().build();
	}
}
