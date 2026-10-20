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
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import rs.formuvia.common.dto.HistoryDTO;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.model.dto.ObjectFormDTO;
import rs.formuvia.model.service.ModelPreviewService;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.ErrorDetail;

@Path("")
@Tag(name = "Model / Preview", description = "Reading data from tables defined through the model. Unlike the built-in tables, these have no DTO class of their own: the columns, their labels and the permissions are taken from the model, so one endpoint serves every table that has been created this way. Access is therefore not tied to a fixed role but to the view role of the model being read.")
public class ModelPreviewController {

	@Inject
	private ModelPreviewService modelPreviewService;

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.modelPreviewTable)
	@Operation(operationId = "getModelPreviewTable", summary = "List rows of a model table", description = "Returns one page of rows from the table of the given model. Request: pageIndex (zero-based), pageSize (all rows if omitted), filters (field = column name of the field, searchOperation, value in field1, upper bound in field2 for BETWEEN) and orders (fieldName, direction ASC or DESC); without orders the rows are sorted by identifier, newest first. Response: name and description (the model's own name and description, translated), column (only the fields marked to be shown in the table), allColumns (the identifier plus every field of the model, each with its translated label, data type, whether it is editable and whether it is required), list (rows as field name to value), total (number of matching rows) and numberOfPages. A field linked to a codebook also carries listOfValues, the records of that codebook with the label built from the fields marked as part of it. A field of type FILE holds the identifier of the stored file in the row, and the names of the files on this page come in the listOfValues of that column, so the client can show the name instead of the identifier; such a column cannot be filtered or sorted by. Labels are translated to the language from the X-Language header.", requestBody = @RequestBody(description = "Page, page size, filters and sort order", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseParameter.class))), responses = {
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
	@Operation(operationId = "getModelPreviewForm", summary = "Get an empty form of a model", description = "Returns the fields of the entry form for a new record of the given model. Every field carries its code, its translated label, the data type, the length, whether it is required and whether it may be changed, and its place in the dialog (row, column and width in columns), so the client can draw the form exactly as it was designed. A field linked to a codebook carries listOfValues with the records of that codebook; a field with its own query carries the values that query returns. When that codebook is itself a subtable, the field also carries parentListOfValues, the levels above it, and the records of those levels come once each in parentCodebook, keyed by the model they belong to. A field of type FILE carries its value as an object with the identifier of the stored file in id and its original name in fileName, so the form can show the name and offer the file for download. Fields with a default value query come back already filled. The first item is the identifier, which is empty for a new record. Requires a valid session and the view role of the model.", responses = {
			@ApiResponse(responseCode = "200", description = "Fields of the empty form, with the records of the codebooks above them", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ObjectFormDTO.class))),
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
			@ApiResponse(responseCode = "200", description = "Fields of the form filled with the values of the record, with the records of the codebooks above them", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ObjectFormDTO.class))),
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
			@ApiResponse(responseCode = "200", description = "Fields of the empty form with the link to the parent record, and the records of the codebooks above them", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ObjectFormDTO.class))),
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
	@Operation(operationId = "getModelPreviewUpdate", summary = "Add or change a record", description = "Stores one record of the table of the given model. The body is the record itself, field code to value, as the form describes it: the identifier under id, empty for a new record and filled when an existing one is changed; the link to the record of the parent table under parent, for a subtable; and every field of the form under its own code. Values follow the data type of the field, a date as yyyy-MM-dd, a date and time as yyyy-MM-ddTHH:mm, a field linked to a codebook as the identifier of the chosen record, and an empty field as null. A field of type FILE is sent as an object: fileUploadFile is the identifier returned by the file upload when a new file was chosen, and the server then stores that file with the record; id is the file already stored, sent back unchanged when the file was not touched, and fileName is the original name of the file. Null clears the file. Fields the form marks as not editable are sent back unchanged. The stored record is returned, with the identifier of a newly added one, so the client can refresh that row without reading the whole table again. Adding requires the add role of the model, changing the update role.", requestBody = @RequestBody(description = "Record as field code to value, with id empty for a new record", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = "object", description = "Field code to value"))), responses = {
			@ApiResponse(responseCode = "200", description = "Stored record, with its identifier", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = "object", description = "Field code to value"))),
			@ApiResponse(responseCode = "400", description = "A required field is empty, a value does not fit the type or the length of its field, or the record being changed no longer exists", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the role for adding or changing data in this model", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getUpdate(LinkedHashMap<String, Object> object,
			@Parameter(description = "Identifier of the model whose record is stored", required = true) @PathParam("modelId") UUID modelId) {
		return Response.ok(modelPreviewService.getUpdate(modelId, object)).build();
	}

	@DELETE
	@Path(ApiRoute.modelPreviewDelete)
	@Operation(operationId = "getModelPreviewDelete", summary = "Delete a record", description = "Deletes one record from the table of the given model. Rows of its subtables go with it, because the link to the parent record is created with ON DELETE CASCADE; a record another table points to as a codebook value cannot be deleted, and the request is refused. Requires a valid session and the delete role of the model.", responses = {
			@ApiResponse(responseCode = "204", description = "Record deleted"),
			@ApiResponse(responseCode = "400", description = "Unknown model, or the record cannot be deleted because another table points to it", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the delete role of this model", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getDelete(
			@Parameter(description = "Identifier of the model whose record is deleted", required = true) @PathParam("modelId") UUID modelId,
			@Parameter(description = "Identifier of the record to delete", required = true) @PathParam("id") UUID id) {
		modelPreviewService.getDelete(modelId, id);
		return Response.noContent().build();
	}

	@GET
	@Path(ApiRoute.modelPreviewHistory)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getModelPreviewHistory", summary = "Get history of a record", description = "Returns the changes of one record of a model table, newest first, the same way the history of a built-in table is returned: the action (added, changed or deleted, translated to the language of the request), the time, the user that made the change, and the fields that actually changed with their old and new value. Field names are the labels from the model, and values are read according to the data type of the column, so dates, numbers and codebook values come back in the same shape as in the table. Requires a valid session and the view role of the model.", responses = {
			@ApiResponse(responseCode = "200", description = "Changes of the record, newest first; an empty list when nothing has been changed yet", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = HistoryDTO.class)))),
			@ApiResponse(responseCode = "400", description = "Unknown model, or the history cannot be read", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the view role of this model", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getHistory(
			@Parameter(description = "Identifier of the model whose record is read", required = true) @PathParam("modelId") UUID modelId,
			@Parameter(description = "Identifier of the record whose history is read", required = true) @PathParam("id") UUID id) {

		return Response.ok(modelPreviewService.getHistory(modelId, id)).build();
	}

	@GET
	@Path(ApiRoute.modelPreviewRow)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getModelPreviewRow", summary = "Get one row of a model table", description = "Returns a single record of the table of the given model, in the same shape as a row of the table listing: field code to value, with the identifier and, for a subtable, the link to the parent record. The client uses it to refresh one row without reading the whole page again, for instance when coming back from a subtable. Requires a valid session and the view role of the model.", responses = {
			@ApiResponse(responseCode = "200", description = "The record, as one row of the table", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = "object", description = "Field code to value"))),
			@ApiResponse(responseCode = "400", description = "Unknown model, or the record no longer exists", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the view role of this model", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getRow(
			@Parameter(description = "Identifier of the model whose record is read", required = true) @PathParam("modelId") UUID modelId,
			@Parameter(description = "Identifier of the record", required = true) @PathParam("id") UUID id) {

		return Response.ok(modelPreviewService.getRow(modelId, id)).build();
	}

	@GET
	@Path(ApiRoute.modelDownloadFile)
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Operation(operationId = "getModelPreviewFile", summary = "Download the file of a record", description = "Returns the file stored in one column of one record of a model table. The column must be of type FILE; its value is the identifier of the stored file, from which the file itself, its original name and its content type are read. The name is returned in the Content-Disposition header and the content type is the one detected when the file was stored, so the browser saves it under the name the user gave it. Requires a valid session and the view role of the model.", responses = {
			@ApiResponse(responseCode = "200", description = "Content of the file, with its name in the Content-Disposition header", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM, schema = @Schema(type = "string", format = "binary"))),
			@ApiResponse(responseCode = "400", description = "The table has no such column, the record does not exist, it holds no file, or the file is missing from the storage", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the view role of this model", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getDownloadFile(
			@Parameter(description = "Identifier of the model the record belongs to", required = true) @PathParam("modelId") UUID modelId,
			@Parameter(description = "Identifier of the record", required = true) @PathParam("id") UUID id,
			@Parameter(description = "Code of the column of type FILE the file is stored in", required = true) @PathParam("columnName") String columnName) {

		return modelPreviewService.getDownloadFile(modelId, id, columnName);
	}

	@GET
	@Path(ApiRoute.modelPreviewDownloadTemplate)
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Operation(operationId = "getModelPreviewTemplate", summary = "Download the entry template of a model table", description = "Returns an empty Excel file (.xlsx) for entering data into the table of the given model, named after the model. The first row carries the translated label of each column, red when the field is required; which field a column belongs to is written on a hidden sheet of the file, as the position of the column and the identifier of the field, so the import must find the columns in the order the template was made with. Only the fields that may be changed are taken, without the ones holding a file, in the order they stand on the entry form. Each column gets the format and the width of its data type, and a check on what may be typed: a number, a date, a time, or a value chosen from a list. Fields linked to a codebook, a list of values or yes/no are offered as a drop-down whose values sit on hidden sheets of the same file. The sheet is protected so the labels and the codes cannot be overwritten, while the entry area stays open. The file name is in the Content-Disposition header, which is exposed to the browser for cross-origin requests.", responses = {
			@ApiResponse(responseCode = "200", description = "Excel template, with the file name in the Content-Disposition header", content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", schema = @Schema(type = "string", format = "binary"))),
			@ApiResponse(responseCode = "400", description = "Unknown model", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the role this model requires for the template", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getModelTemplateDownload(
			@Parameter(description = "Identifier of the model whose template is read", required = true) @PathParam("modelId") UUID modelId) {

		return modelPreviewService.getModelTemplateDownload(modelId);
	}

	@POST
	@Path(ApiRoute.modelPreviewUploadTemplate)
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "postModelPreviewTemplate", summary = "Import a filled-in template", description = "Reads a filled-in template and writes its rows into the table of the given model. The file is sent beforehand through the file upload, so only its identifier travels here. The file must be the template of this very table: the sheet with the data, the sheet that says which column belongs to which field, and the sheets with the offered values are all read from it, so the import does not depend on the language or on the names in the header. Every row becomes a record, exactly as if it had been entered on the form - the same required fields, the same conversions and the same roles. The whole file is one transaction: the first row the server cannot accept stops the import, nothing is written, and the answer says which row it was and what is wrong with it.", responses = {
			@ApiResponse(responseCode = "204", description = "Every row was written"),
			@ApiResponse(responseCode = "400", description = "The file is not an Excel workbook, a sheet or a column the template needs is missing, or a row holds a value the table cannot take; the message says which row", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the role this model requires for the import", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getModelTemplateUpload(
			@Parameter(description = "Identifier of the model the rows are written into", required = true) @PathParam("modelId") UUID modelId,
			@Parameter(description = "Identifier the file upload returned for the filled-in template", required = true) @PathParam("fileTemplateId") UUID fileTemplateId) {

		modelPreviewService.getModelTemplateUpload(fileTemplateId, modelId, null);
		return Response.noContent().build();
	}

	@POST
	@Path(ApiRoute.modelPreviewUploadTemplateWithParent)
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "postModelPreviewTemplateByParent", summary = "Import a filled-in template into a subtable", description = "The same as importing a template, except that every row read from the file is tied to one record of the parent table, the way a subtable requires. The link to the parent is not in the file; it is taken from the path, so the same template can be filled in once and imported under different parent records.", responses = {
			@ApiResponse(responseCode = "204", description = "Every row was written under the given parent record"),
			@ApiResponse(responseCode = "400", description = "The file is not an Excel workbook, a sheet or a column the template needs is missing, the model is not a subtable, or a row holds a value the table cannot take; the message says which row", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the role this model requires for the import", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getModelTemplateUpload(
			@Parameter(description = "Identifier of the model the rows are written into", required = true) @PathParam("modelId") UUID modelId,
			@Parameter(description = "Identifier the file upload returned for the filled-in template", required = true) @PathParam("fileTemplateId") UUID fileTemplateId,
			@Parameter(description = "Identifier of the record in the parent table the rows belong to", required = true) @PathParam("parentId") UUID parentId) {
		modelPreviewService.getModelTemplateUpload(fileTemplateId, modelId, parentId);
		return Response.noContent().build();
	}

	@GET
	@Path(ApiRoute.modelListFileVersion)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getModelPreviewFileVersions", summary = "List the versions of the file of a record", description = "Returns every version of the file stored in one column of one record, newest first. A file is replaced in place: the record keeps pointing at the same stored file, and each file that was put there is written as a version of it, with its original name, its content type and the time it was stored. The answer has the same shape as any other table - the columns with their translated labels and the rows - so the client can show it without knowing anything about versions. Requires a valid session and the view role of the model.", responses = {
			@ApiResponse(responseCode = "200", description = "Versions of the file, newest first", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseTable.class))),
			@ApiResponse(responseCode = "400", description = "The table has no such column, the record does not exist, or it holds no file", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the view role of this model", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getListFileVersion(
			@Parameter(description = "Identifier of the model the record belongs to", required = true) @PathParam("modelId") UUID modelId,
			@Parameter(description = "Identifier of the record", required = true) @PathParam("id") UUID id,
			@Parameter(description = "Code of the column of type FILE the file is stored in", required = true) @PathParam("columnName") String columnName) {

		return Response.ok(modelPreviewService.getListFileVersion(modelId, id, columnName)).build();
	}

	@GET
	@Path(ApiRoute.modelDownloadFileVersion)
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Operation(operationId = "getModelPreviewFileVersion", summary = "Download one version of the file of a record", description = "Returns the content of one earlier version of the file stored in a column of a record, under the name and the content type it had when it was stored. The version must belong to the file this record holds, so a version of another file cannot be read through this record. The file name is in the Content-Disposition header, which is exposed to the browser for cross-origin requests. Requires a valid session and the view role of the model.", responses = {
			@ApiResponse(responseCode = "200", description = "Content of that version, with its name in the Content-Disposition header", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM, schema = @Schema(type = "string", format = "binary"))),
			@ApiResponse(responseCode = "400", description = "The table has no such column, the record holds no file, the version does not exist or belongs to another file, or the file is missing from the storage", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the view role of this model", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getDownloadFileVersion(
			@Parameter(description = "Identifier of the model the record belongs to", required = true) @PathParam("modelId") UUID modelId,
			@Parameter(description = "Identifier of the record", required = true) @PathParam("id") UUID id,
			@Parameter(description = "Code of the column of type FILE the file is stored in", required = true) @PathParam("columnName") String columnName,
			@Parameter(description = "Identifier of the version to download", required = true) @PathParam("versionId") UUID versionId) {

		return modelPreviewService.getDownloadFileVersion(modelId, id, columnName, versionId);
	}

	@DELETE
	@Path(ApiRoute.modelDownloadFileVersion)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "deleteModelPreviewFileVersion", summary = "Delete one version of the file of a record", description = "Removes one earlier version of the file stored in a column of a record. The version must belong to the file this record holds. The last version cannot be removed, because the record would be left pointing at a file that is no longer described anywhere; when the version being removed is the one the record points at, the record is moved to the version stored before it. What is left on disk is not touched.", responses = {
			@ApiResponse(responseCode = "204", description = "The version was removed"),
			@ApiResponse(responseCode = "400", description = "The table has no such column, the record holds no file, the version does not exist or belongs to another file, or it is the last version of that file", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the role this model requires for it", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getDeleteFileVersion(
			@Parameter(description = "Identifier of the model the record belongs to", required = true) @PathParam("modelId") UUID modelId,
			@Parameter(description = "Identifier of the record", required = true) @PathParam("id") UUID id,
			@Parameter(description = "Code of the column of type FILE the file is stored in", required = true) @PathParam("columnName") String columnName,
			@Parameter(description = "Identifier of the version to delete", required = true) @PathParam("versionId") UUID versionId) {

		modelPreviewService.getDeleteFileVersion(modelId, id, columnName, versionId);
		return Response.noContent().build();
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.modelPreviewTableTotal)
	@Operation(operationId = "getModelPreviewTableTotal", summary = "Sums of the number columns of a model table", description = "Returns the sum of every column of type BIGDECIMAL of the table, over all rows that match the filters sent in the request, not only the page. The request is the same one the table itself is read with, so the sums belong to the list the reader is looking at; pageIndex and pageSize are ignored. The response is an object whose keys are the column names of those fields and whose values are the sums. It exists so that a client that changes one row in place, without reading the page again, can bring the sums up to date on their own.", requestBody = @RequestBody(description = "The same page, filters and sort order the table was read with", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseParameter.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Column name to the sum of that column", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = "object", description = "Sum by column name", example = "{\"iznos\": 12500.00, \"porez\": 2500.00}"))),
			@ApiResponse(responseCode = "400", description = "Unknown field or invalid value in a filter", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the view role of this model", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getTableTotal(DatabaseParameter databaseParameter,
			@Parameter(description = "Identifier of the model whose sums are read", required = true) @PathParam("modelId") UUID modelId) {
		return Response.ok(modelPreviewService.getTableTotal(databaseParameter, modelId, null)).build();
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.modelPreviewTableWithParentTotal)
	@Operation(operationId = "getModelPreviewTableTotalByParent", summary = "Sums of the number columns of a subtable", description = "The same as the sums of a model table, except that only the rows belonging to one record of the parent table are counted: a filter on the parent column is added to the ones sent in the request.", requestBody = @RequestBody(description = "The same page, filters and sort order the subtable was read with", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseParameter.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Column name to the sum of that column, over the rows of the given parent record", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = "object", description = "Sum by column name", example = "{\"iznos\": 12500.00, \"porez\": 2500.00}"))),
			@ApiResponse(responseCode = "400", description = "Unknown field or invalid value in a filter, or the model has no parent column because it is not a subtable", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the view role of this model", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getTableTotal(DatabaseParameter databaseParameter,
			@Parameter(description = "Identifier of the model whose subtable sums are read", required = true) @PathParam("modelId") UUID modelId,
			@Parameter(description = "Identifier of the record in the parent table whose rows are counted", required = true) @PathParam("parentId") UUID parentId) {
		return Response.ok(modelPreviewService.getTableTotal(databaseParameter, modelId, parentId)).build();
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.modelChangeColumn)
	@Operation(operationId = "getModelPreviewColumnChange", summary = "Values worked out by the database after a field is changed", description = "Hands the record, as it is filled in at that moment, to the function of the database the model puts on that field (the change function of the column), and returns the record the function gives back. It is what lets one field fill in or correct others, for example a tax worked out from a rate. The request is an object of column name to value, the whole record and not only the field that changed; the identifier of the record is in it when the record already exists, and the link to the parent row when the table is a subtable. The function takes one argument of type json and returns json, so a key the table does not have is ignored, and a column the answer says nothing about keeps the value it had. Nothing is stored here: the answer is only shown, and it reaches the database when the record itself is saved.", requestBody = @RequestBody(description = "The record as it stands, column name to value", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = "object", description = "Value by column name", example = "{\"id\": null, \"stopa\": 20, \"osnovica\": 1000}"))), responses = {
			@ApiResponse(responseCode = "200", description = "The record the function returned, column name to value", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = "object", description = "Value by column name", example = "{\"stopa\": 20, \"osnovica\": 1000, \"porez\": 200}"))),
			@ApiResponse(responseCode = "400", description = "The table has no such column, no change function is set on it, or the function returned nothing", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the role this model requires for a change", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getColumnChange(LinkedHashMap<String, Object> object,
			@Parameter(description = "Identifier of the model the record belongs to", required = true) @PathParam("modelId") UUID modelId,
			@Parameter(description = "Code of the column that was changed; the change function is read from it", required = true) @PathParam("columnName") String columnName) {
		return Response.ok(modelPreviewService.getColumnChange(object, modelId, columnName)).build();
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.modelExecuteProcedure)
	@Operation(operationId = "executeModelProcedure", summary = "Run a procedure of the table over one record", description = "Runs one of the procedures the model put on a table over a single record: the procedure is called with the identifier of that record as its only argument, and whatever it changes it changes itself. There is no request body. The answer is that record as it stands after the procedure, column name to value, in the same shape as one row of the table, so the list can draw that row again without being loaded anew; a file column carries the stored file with its name and type. The procedure says which table it belongs to, so the identifier of the model is not given. It asks for the role the model requires for a change, because a procedure changes the record.", responses = {
			@ApiResponse(responseCode = "200", description = "The record as it stands after the procedure, column name to value", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = "object", description = "Value by column name", example = "{\"id\": \"0190e0a0-0000-7000-8000-000000000000\", \"osnovica\": 1000, \"porez\": 200}"))),
			@ApiResponse(responseCode = "400", description = "No procedure or record with the given identifier, or the procedure itself failed", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the role this model requires for a change", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getExecuteProcedure(
			@Parameter(description = "Identifier of the procedure of the table, as the table sends it with its rows", required = true) @PathParam("procedureId") UUID procedureId,
			@Parameter(description = "Identifier of the record the procedure is run over", required = true) @PathParam("id") UUID id) {
		return Response.ok(modelPreviewService.getExecuteProcedure(procedureId, id)).build();
	}

	@GET
	@Path(ApiRoute.modelJasperPrint)
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Operation(operationId = "printModelJasper", summary = "Make a report of the table over one record", description = "Makes one of the reports the model put on a table over a single record and returns it as a PDF file. The report is the compiled .jasper file kept with that entry; it is filled on the connection of this request, so the query of the report reads the database directly and sees the record as it stands, and it is handed the identifier of that record as its parameter id, together with the language of the request as the locale of the report. The answer is the finished PDF, named after the report, with the name in the Content-Disposition header, which is exposed to the browser for cross-origin requests. The report says which table it belongs to, so the identifier of the model is not given. It asks for the view role of the model, because making a report only reads the record.", responses = {
			@ApiResponse(responseCode = "200", description = "PDF of the report, with the file name in the Content-Disposition header", content = @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary"))),
			@ApiResponse(responseCode = "400", description = "No report with that identifier on this table, its stored file is missing, or the report itself could not be made", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the view role of this model", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getPrintModelJasper(
			@Parameter(description = "Identifier of the report of the table, as the table sends it with its rows", required = true) @PathParam("modelJasperId") UUID modelJasperId,
			@Parameter(description = "Identifier of the record the report is made over", required = true) @PathParam("id") UUID id) {

		return modelPreviewService.getPrintModelJasper(modelJasperId, id);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.modelPreviewFormFromTable)
	@Operation(operationId = "getFormForTable", summary = "The record of a codebook, reached from a table that points at it", description = "Returns the form of one record of a codebook, in the same shape as the form of a record of a table, so a value picked from a codebook can be looked at without leaving the table it stands in. The right to read it is judged by the table it is reached from, not by the codebook: it asks for the view role of that table, and it is given only if a column of that table really points at this codebook. The record itself is only shown, never changed through this path.", responses = {
			@ApiResponse(responseCode = "200", description = "Fields of the record of the codebook, with the records of the codebooks above them", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ObjectFormDTO.class))),
			@ApiResponse(responseCode = "400", description = "No table or codebook with the given identifier, no record with that identifier, or no column of the table points at that codebook", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the view role of the table the record is reached from", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getFormForTable(
			@Parameter(description = "Identifier of the table the codebook value stands in", required = true) @PathParam("currentModelId") UUID currentModelId,
			@Parameter(description = "Identifier of the codebook the record belongs to", required = true) @PathParam("modelId") UUID modelId,
			@Parameter(description = "Identifier of the record of the codebook", required = true) @PathParam("id") UUID id) {
		return Response.ok(modelPreviewService.getFormForTable(currentModelId, modelId, id)).build();
	}
}
