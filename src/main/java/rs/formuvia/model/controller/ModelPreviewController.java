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
	@Operation(
			operationId = "getModelPreviewForm",
			summary = "Get an empty form of a model",
			description = "Returns the fields of the entry form for a new record of the given model. Every field carries its code, its translated label, the data type, the length, whether it is required and whether it may be changed, and its place in the dialog (row, column and width in columns), so the client can draw the form exactly as it was designed. A field linked to a codebook carries listOfValues with the records of that codebook; a field with its own query carries the values that query returns. Fields with a default value query come back already filled. The first item is the identifier, which is empty for a new record. Requires a valid session and the view role of the model.",
			responses = {
			@ApiResponse(responseCode = "200", description = "Fields of the empty form",
					content = @Content(mediaType = MediaType.APPLICATION_JSON,
							array = @ArraySchema(schema = @Schema(implementation = ModelColumnPreviewDTO.class)))),
			@ApiResponse(responseCode = "400", description = "Unknown model, or the record does not exist",
					content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session",
					content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the view role of this model",
					content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getForm(
			@Parameter(description = "Identifier of the model whose form is read", required = true) @PathParam("modelId") UUID modelId) {
		return Response.ok(modelPreviewService.getForm(modelId, null, null)).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.modelPreviewFormWithId)
	@Operation(
			operationId = "getModelPreviewFormById",
			summary = "Get the form of one record",
			description = "The same as the empty form, but every field carries the value of the given record. Default values and field queries are not run here, because they belong to entering a new record; a field linked to a codebook still carries the codebook records, so the client can show the label instead of the identifier.",
			responses = {
			@ApiResponse(responseCode = "200", description = "Fields of the form, filled with the values of the record",
					content = @Content(mediaType = MediaType.APPLICATION_JSON,
							array = @ArraySchema(schema = @Schema(implementation = ModelColumnPreviewDTO.class)))),
			@ApiResponse(responseCode = "400", description = "Unknown model, or the record does not exist",
					content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session",
					content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the view role of this model",
					content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getForm(
			@Parameter(description = "Identifier of the model whose form is read", required = true) @PathParam("modelId") UUID modelId,
			@Parameter(description = "Identifier of the record whose values fill the form", required = true) @PathParam("id") UUID id) {
		return Response.ok(modelPreviewService.getForm(modelId, id, null)).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.modelPreviewFormWithIdAndParent)
	@Operation(
			operationId = "getModelPreviewFormByIdAndParent",
			summary = "Get the form of a subtable record",
			description = "The same as the form of one record, with one field added: the link to the record of the parent table. This is what a subtable uses, so that a new row already knows which parent record it belongs to.",
			responses = {
			@ApiResponse(responseCode = "200", description = "Fields of the form, with the link to the parent record",
					content = @Content(mediaType = MediaType.APPLICATION_JSON,
							array = @ArraySchema(schema = @Schema(implementation = ModelColumnPreviewDTO.class)))),
			@ApiResponse(responseCode = "400", description = "Unknown model, or the record does not exist",
					content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session",
					content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the view role of this model",
					content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getForm(
			@Parameter(description = "Identifier of the model whose form is read", required = true) @PathParam("modelId") UUID modelId,
			@Parameter(description = "Identifier of the record whose values fill the form", required = true) @PathParam("id") UUID id,
			@Parameter(description = "Identifier of the record in the parent table", required = true) @PathParam("parent") UUID parent) {
		return Response.ok(modelPreviewService.getForm(modelId, id, parent)).build();
	}
}
