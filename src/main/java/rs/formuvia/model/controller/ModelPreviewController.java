package rs.formuvia.model.controller;

import java.util.LinkedHashMap;
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
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.model.dto.ModelColumnPreviewDTO;
import rs.formuvia.model.service.ModelPreviewService;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.ErrorDetail;

@Path("")
@Tag(name = "Model preview", description = "Reading data from tables defined through the model. Unlike the built-in tables, these have no DTO class of their own: the columns, their labels and the permissions are taken from the model, so one endpoint serves every table that has been created this way. Access is therefore not tied to a fixed role but to the view role of the model being read.")
public class ModelPreviewController {

	@Inject
	private ModelPreviewService modelPreviewService;

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.modelPreviewTable)
	@Operation(operationId = "getModelPreviewTable", summary = "List rows of a model table", description = "Returns one page of rows from the table of the given model. Request: pageIndex (zero-based), pageSize (all rows if omitted), filters (field = column name of the field, searchOperation, value in field1, upper bound in field2 for BETWEEN) and orders (fieldName, direction ASC or DESC); without orders the rows are sorted by identifier, newest first. Response: name and description (the model's own name and description, translated), column (only the fields marked to be shown in the table), allColumns (the identifier plus every field of the model, each with its translated label, data type, whether it is editable and whether it is required), list (rows as field name to value), total (number of matching rows) and numberOfPages. A field linked to a codebook also carries listOfValues, the records of that codebook with the label built from the fields marked as part of it. Labels are translated to the language from the X-Language header.", requestBody = @RequestBody(description = "Page, page size, filters and sort order", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseParameter.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Page of rows", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseTable.class))),
			@ApiResponse(responseCode = "400", description = "Unknown field or invalid value in a filter or sort", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the view role of this model", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getTable(DatabaseParameter databaseParameter,
			@Parameter(description = "Identifier of the model whose table is read", required = true) @PathParam("modelId") UUID modelId) {
		return Response.ok(modelPreviewService.getTable(databaseParameter, modelId, null)).build();
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.modelPreviewTableWithParent)
	@Operation(operationId = "getModelPreviewTableByParent", summary = "List rows of a subtable", description = "The same as listing a model table, except that only the rows belonging to one record of the parent table are returned: a filter on the parent column is added to the ones sent in the request. This is what a subtable uses, where a row of the parent table opens its own children. The parent column itself is left out of the response, both from the columns and from the rows, because it is the same for every row and says nothing to the reader.", requestBody = @RequestBody(description = "Page, page size, filters and sort order", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseParameter.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Page of rows belonging to the given parent record", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseTable.class))),
			@ApiResponse(responseCode = "400", description = "Unknown field or invalid value in a filter or sort, or the model has no parent column because it is not a subtable", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the view role of this model", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getTable(DatabaseParameter databaseParameter,
			@Parameter(description = "Identifier of the model whose subtable is read", required = true) @PathParam("modelId") UUID modelId,
			@Parameter(description = "Identifier of the record in the parent table whose rows are returned", required = true) @PathParam("parentId") UUID parentId) {
		return Response.ok(modelPreviewService.getTable(databaseParameter, modelId, parentId)).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.modelPreviewForm)
	@Operation(operationId = "getModelPreviewForm", summary = "Get an empty form of a model", description = "Returns the fields of the entry form for a new record of the given model. Every field carries its code, its translated label, the data type, the length, whether it is required and whether it may be changed, and its place in the dialog (row, column and width in columns), so the client can draw the form exactly as it was designed. A field linked to a codebook carries listOfValues with the records of that codebook; a field with its own query carries the values that query returns. Fields with a default value query come back already filled. The first item is the identifier, which is empty for a new record. Requires a valid session and the view role of the model.", responses = {
			@ApiResponse(responseCode = "200", description = "Fields of the empty form", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = ModelColumnPreviewDTO.class)))),
			@ApiResponse(responseCode = "400", description = "Unknown model, or the record does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the view role of this model", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getForm(
			@Parameter(description = "Identifier of the model whose form is read", required = true) @PathParam("modelId") UUID modelId) {
		return Response.ok(modelPreviewService.getForm(modelId, null, null)).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.modelPreviewFormWithId)
	@Operation(operationId = "getModelPreviewFormById", summary = "Get the form of one record", description = "The same as the empty form, but every field carries the value of the given record. Default values and field queries are not run here, because they belong to entering a new record; a field linked to a codebook still carries the codebook records, so the client can show the label instead of the identifier.", responses = {
			@ApiResponse(responseCode = "200", description = "Fields of the form, filled with the values of the record", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = ModelColumnPreviewDTO.class)))),
			@ApiResponse(responseCode = "400", description = "Unknown model, or the record does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the view role of this model", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getForm(
			@Parameter(description = "Identifier of the model whose form is read", required = true) @PathParam("modelId") UUID modelId,
			@Parameter(description = "Identifier of the record whose values fill the form", required = true) @PathParam("id") UUID id) {
		return Response.ok(modelPreviewService.getForm(modelId, id, null)).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.modelPreviewFormWithParent)
	@Operation(operationId = "getModelPreviewFormByParent", summary = "Get an empty form of a subtable", description = "Returns the empty form for a new record of a subtable: the same fields as the empty form of the model, plus the link to the record of the parent table, already filled in, so the new row knows which record it belongs to. Default values and field queries run here as they do for any new record. An existing record of a subtable is read through the form of one record, because its link to the parent is already stored.", responses = {
			@ApiResponse(responseCode = "200", description = "Fields of the empty form, with the link to the parent record", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = ModelColumnPreviewDTO.class)))),
			@ApiResponse(responseCode = "400", description = "Unknown model, or the record does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the view role of this model", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getFormWithParent(
			@Parameter(description = "Identifier of the model whose form is read", required = true) @PathParam("modelId") UUID modelId,
			@Parameter(description = "Identifier of the record in the parent table", required = true) @PathParam("parent") UUID parent) {
		return Response.ok(modelPreviewService.getForm(modelId, null, parent)).build();
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.modelPreviewUpdate)
	@Operation(operationId = "getModelPreviewUpdate", summary = "Add or change a record", description = "Stores one record of the table of the given model. The body is the record itself, field code to value, as the form describes it: the identifier under id, empty for a new record and filled when an existing one is changed; the link to the record of the parent table under parent, for a subtable; and every field of the form under its own code. Values follow the data type of the field, a date as yyyy-MM-dd, a date and time as yyyy-MM-ddTHH:mm, a field linked to a codebook as the identifier of the chosen record, and an empty field as null. Fields the form marks as not editable are sent back unchanged. The stored record is returned, with the identifier of a newly added one, so the client can refresh that row without reading the whole table again. Adding requires the add role of the model, changing the update role.", requestBody = @RequestBody(description = "Record as field code to value, with id empty for a new record", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = "object", description = "Field code to value"))), responses = {
			@ApiResponse(responseCode = "200", description = "Stored record, with its identifier", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = "object", description = "Field code to value"))),
			@ApiResponse(responseCode = "400", description = "A required field is empty, a value does not fit the type or the length of its field, or the record being changed no longer exists", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the role for adding or changing data in this model", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getUpdate(LinkedHashMap<String, Object> object,
			@Parameter(description = "Identifier of the model whose record is stored", required = true) @PathParam("modelId") UUID modelId) {
		return Response.ok(modelPreviewService.getUpdate(modelId, object)).build();
	}
}
