package rs.formuvia.common.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import rs.formuvia.common.service.FunctionListService;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.ErrorDetail;

@Path("")
@Tag(name = "Common / Database routines", description = "Names of the routines the database itself holds, read from information_schema.routines of the current schema. The form design uses them to put a function behind a field of a form: only a function that takes the record as json and returns json can be used there, so the list is asked for the FUNCTION kind returning json.")
public class FunctionListController {

	@Inject
	private FunctionListService functionListService;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.listAllFunctions)
	@Operation(operationId = "getAllFunctions", summary = "Names of the routines of one kind", description = "Returns the names of every routine of the given kind in the current schema, sorted by name, whatever data type it returns. The kind is the routine_type of information_schema.routines, so FUNCTION or PROCEDURE, written in capitals as the database holds it.", responses = {
			@ApiResponse(responseCode = "200", description = "Names of the routines, sorted", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(type = "string", example = "calculate_tax")))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getAllFuncions(
			@Parameter(description = "Kind of routine, as routine_type holds it: FUNCTION or PROCEDURE", required = true, example = "FUNCTION") @PathParam("functionType") String functionType) {
		return Response.ok(functionListService.getAllFunction(functionType, null)).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.listAllFunctionsWithDataType)
	@Operation(operationId = "getAllFunctionsByDataType", summary = "Names of the routines of one kind returning one data type", description = "The same as the list of routines of one kind, narrowed to those that return the given data type. The type is matched the way the database writes it in the data_type column of information_schema.routines, and it is matched as a pattern, so json finds only json while json% finds jsonb as well. This is what the form design asks for FUNCTION and json, because a function behind a field has to return json.", responses = {
			@ApiResponse(responseCode = "200", description = "Names of the routines, sorted", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(type = "string", example = "calculate_tax")))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getAllFuncions(
			@Parameter(description = "Kind of routine, as routine_type holds it: FUNCTION or PROCEDURE", required = true, example = "FUNCTION") @PathParam("functionType") String functionType,
			@Parameter(description = "Data type the routine returns, as data_type holds it; matched as a pattern", required = true, example = "json") @PathParam("dataType") String dataType) {
		return Response.ok(functionListService.getAllFunction(functionType, dataType)).build();
	}
}
