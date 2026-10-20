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
import rs.formuvia.model.dto.ModelExtendedDTO;
import rs.formuvia.model.dto.ModelJasperDTO;
import rs.formuvia.model.dto.ModelProcedureDTO;
import rs.formuvia.model.dto.ModelTreeDTO;
import rs.formuvia.model.dto.ModelTriggerDTO;
import rs.formuvia.model.service.ModelService;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.ErrorDetail;
import rs.formuvia.utils.RoleList;

@Path("")
@Tag(name = "Model / Structure", description = "Definition of the application structure as a tree: menus at the first level, tables under menus and subtables under tables. Saving a new table creates it in the database; deleting a table drops it together with its data. All endpoints require a valid session and the admin role.")
public class ModelController {

	@Inject
	private ModelService modelService;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.modelTree)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "Get model tree", description = "Returns the whole model as one tree. The root is not a stored item: it has no identifier and its name is translated to the language from the X-Language header. Its children are the menus, their children the tables, and tables can have subtables to any depth. Items on every level are sorted by name.", responses = {
			@ApiResponse(responseCode = "200", description = "Model tree", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ModelTreeDTO.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getTree() {
		return Response.ok(modelService.getTree()).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.modelId)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "Get model item", description = "Returns one menu or table by identifier, used to fill the edit form. A table also carries the procedures of the database put on it, sorted by name, each with the name it was given and the procedure it stands for, and the triggers of the database standing on it, sorted by name as well.", responses = {
			@ApiResponse(responseCode = "200", description = "Model item, with the procedures and the triggers of the table", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ModelExtendedDTO.class))),
			@ApiResponse(responseCode = "400", description = "Item with the given identifier does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getModel(
			@Parameter(description = "Model item identifier", required = true) @PathParam("id") UUID id) {
		return Response.ok(modelService.getModel(id)).build();
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.model)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "Create or update model item", description = "Creates a new item when id is empty, otherwise updates the item with that id. Rules: a MENU has no parent, and its code and roles are cleared; a TABLE must have a parent (a menu or another table), a code and all four roles (view, add, edit, delete). The code is the table name in the database: lowercase letters, digits and underscore, starting with a letter, at most 56 characters, unique among items. The name must be unique as well. When a new TABLE is saved, the table is created in the database with an id column; under another table it also gets a parent column with a foreign key that deletes its rows together with the parent row. A TABLE also carries the size of its entry dialog: dialogWidth (at least 400), columnNumber (1 to 12) and rowNumber (at least 1), all three required. The grid may not be made smaller than what the fields already placed in it occupy; the message then carries the last column or row in use. Changing the code of an existing table is ignored, and the stored code is kept, because the table already exists in the database under that name.", requestBody = @RequestBody(description = "Menu or table data; id empty for a new item", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ModelDTO.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Item saved", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ModelDTO.class))),
			@ApiResponse(responseCode = "400", description = "A rule is broken: a menu with a parent, a table without a parent, a missing or invalid code, a missing role, a missing or out of range size of the entry dialog, a grid smaller than the fields already use, a name or code that already exists, or a table that already exists in the database", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getUpdate(@Valid ModelDTO modelDTO) {
		return Response.ok(modelService.getUpdate(modelDTO)).build();
	}

	@DELETE
	@Path(ApiRoute.modelId)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "Delete model item", description = "Deletes a menu or a table. An item that still has child items cannot be deleted; delete the children first. Deleting a TABLE also drops the table from the database together with all its data, which cannot be undone. The response body is empty.", responses = {
			@ApiResponse(responseCode = "204", description = "Item deleted"),
			@ApiResponse(responseCode = "400", description = "Item does not exist or still has child items", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getDelete(
			@Parameter(description = "Model item identifier", required = true) @PathParam("id") UUID id) {
		modelService.getDelete(id);
		return Response.noContent().build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.modelListOfValues)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(summary = "Get the records fields of a table offer", description = "Returns, for one table of the model, the records every field of its form offers a choice from: fields that point at a codebook and fields with a query of their own. The answer is an object whose key is the identifier of the field and whose value is the list of records, each one a stored value and the text shown for it, already translated to the language from the X-Language header. The records of a codebook are the ones held in memory and refreshed when the codebook changes; the records of a field with a query are read by running that query. Fields that offer no choice of their own are not in the answer. It is used by the form design: a condition over such a field is entered by choosing a record instead of by typing a value.", responses = {
			@ApiResponse(responseCode = "200", description = "Field identifier to the records that field offers", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = "object", description = "Field identifier to the list of records, each with a value and the text shown for it"))),
			@ApiResponse(responseCode = "400", description = "A query of a field cannot be run", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getModelListOfValuesColumns(
			@Parameter(description = "Identifier of the table whose fields are read", required = true) @PathParam("id") UUID id) {
		return Response.ok(modelService.getModelListOfValuesColumns(id)).build();
	}

	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.modelUpdateProcedure)
	@RolesAllowed(RoleList.ADMIN)
	@POST
	@Operation(summary = "Create or update a procedure of a table", description = "Ties a procedure of the database to one table of the model under a name of its own, which is what the user is offered in the list of that table. Creates a new one when id is empty, otherwise changes the one with that id. The procedure has to be one the current schema really holds, read from information_schema.routines as a routine of the PROCEDURE kind; a name the database does not know is refused. It also has to take exactly one parameter, the identifier of the record it is run over, because that is how it is called. The name is unique within the table, and it is looked up in the resource bundle before it is shown, so a key may be used instead of plain text and the name then arrives translated.", requestBody = @RequestBody(description = "Procedure of a table; id empty for a new one", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ModelProcedureDTO.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Procedure saved", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ModelProcedureDTO.class))),
			@ApiResponse(responseCode = "400", description = "The database holds no such procedure, the name is already used in this table, or a required value is missing", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getUpdateModelProcedure(@Valid ModelProcedureDTO modelProcedureDTO) {
		return Response.ok(modelService.getUpdateModelProcedure(modelProcedureDTO)).build();
	}

	@Path(ApiRoute.modelUpdateProcedureId)
	@RolesAllowed(RoleList.ADMIN)
	@DELETE
	@Operation(summary = "Delete a procedure of a table", description = "Takes a procedure off the table it was put on. Only the tie to the table is deleted; the procedure itself stays in the database untouched. The response body is empty.", responses = {
			@ApiResponse(responseCode = "204", description = "Procedure taken off the table"),
			@ApiResponse(responseCode = "400", description = "Procedure with the given identifier does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getDeleteModelProcedure(
			@Parameter(description = "Identifier of the procedure of the table", required = true) @PathParam("id") UUID id) {
		modelService.getDeleteModelProcedure(id);
		return Response.noContent().build();
	}

	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.modelUpdateTrigger)
	@RolesAllowed(RoleList.ADMIN)
	@POST
	@Operation(summary = "Create or update a trigger of a table", description = "Puts a trigger of the database on one table of the model and creates it in the database in the same step. Creates a new one when id is empty, otherwise changes the one with that id. The name is the name of the trigger in the database and is unique within the table; it cannot be changed afterwards, because the trigger already exists under it, so a rename is a delete and a new entry. triggerWhen says BEFORE or AFTER the row is written, action what sets it off (INSERT, UPDATE, INSERT_OR_UPDATE, DELETE or ALL for all three), and condition is the WHEN clause of the trigger, written over NEW and OLD and left out when the trigger is to run over every row. The function has to be one the current schema really holds, read from information_schema.routines as a routine of the FUNCTION kind returning trigger; a name the database does not know is refused. A trigger goes on a table only, never on a menu. Nothing in the API runs it afterwards: the database does, on every write of a row.", requestBody = @RequestBody(description = "Trigger of a table; id empty for a new one", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ModelTriggerDTO.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Trigger saved and created in the database", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ModelTriggerDTO.class))),
			@ApiResponse(responseCode = "400", description = "The item is a menu and not a table, the database holds no such function, the name is already used in this table or is being changed, the condition cannot be compiled, or a required value is missing", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getUpdateModelTrigger(@Valid ModelTriggerDTO modelTriggerDTO) {
		return Response.ok(modelService.getUpdateModelTrigger(modelTriggerDTO)).build();
	}

	@Path(ApiRoute.modelUpdateTriggerId)
	@RolesAllowed(RoleList.ADMIN)
	@DELETE
	@Operation(summary = "Delete a trigger of a table", description = "Takes a trigger off the table it was put on and drops it from the database in the same step, so the database stops running it. The function the trigger called stays in the database untouched. The response body is empty.", responses = {
			@ApiResponse(responseCode = "204", description = "Trigger taken off the table and dropped from the database"),
			@ApiResponse(responseCode = "400", description = "Trigger with the given identifier does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getDeleteModelTrigger(
			@Parameter(description = "Identifier of the trigger of the table", required = true) @PathParam("id") UUID id) {
		modelService.getDeleteModelTrigger(id);
		return Response.noContent().build();
	}

	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.modelUpdateJasper)
	@RolesAllowed(RoleList.ADMIN)
	@POST
	@Operation(summary = "Create or update a report of a table", description = "Ties a JasperReports report to one table of the model. Creates a new one when id is empty, otherwise changes the one with that id. The name is what the user sees in the list of reports of that table and is unique within the table. The report itself is a compiled .jasper file, sent beforehand through the file upload and named here under fileUpload by the identifier that upload returned; it is kept for the entry, so a new entry always carries a file, while a change without one keeps the file already stored. The file is read before it is accepted and has to be a report that takes a parameter id of type java.util.UUID, because that is the record the report is run over; anything else is refused. Deleting the entry deletes the stored file with it.", requestBody = @RequestBody(description = "Report of a table; id empty for a new one", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ModelJasperDTO.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Report saved", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ModelJasperDTO.class))),
			@ApiResponse(responseCode = "400", description = "No file is sent for a new entry, the file is not a compiled report, the report has no parameter id of type UUID, the name is already used in this table, or a required value is missing", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getUpdateModelJasper(@Valid ModelJasperDTO modelTriggerDTO) {
		return Response.ok(modelService.getUpdateModelJasper(modelTriggerDTO)).build();
	}

	@Path(ApiRoute.modelUpdateJasperId)
	@RolesAllowed(RoleList.ADMIN)
	@DELETE
	@Operation(summary = "Delete a report of a table", description = "Takes a report off the table it was put on and deletes the stored file of the report with it, so the report disappears from the list of that table. The response body is empty.", responses = {
			@ApiResponse(responseCode = "204", description = "Report taken off the table"),
			@ApiResponse(responseCode = "400", description = "Report with the given identifier does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user does not have the admin role", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getDeleteModelJasper(
			@Parameter(description = "Identifier of the report of the table", required = true) @PathParam("id") UUID id) {
		modelService.getDeleteModelJasper(id);
		return Response.noContent().build();
	}

}
