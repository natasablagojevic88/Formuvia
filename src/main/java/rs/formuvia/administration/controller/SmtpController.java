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
import rs.formuvia.administration.dto.SmtpDTO;
import rs.formuvia.administration.dto.TestMailDTO;
import rs.formuvia.administration.service.SmtpService;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.ErrorDetail;
import rs.formuvia.utils.RoleList;

@Path("")
@Tag(name = "Administration / Mail servers", description = "Servers the application sends mail through: the name it is known by in the application, the address the mail is sent from, the server and its port, the kind of protection of the connection, and the user it signs in as when the server asks for one. Every endpoint asks for the admin role, and the password is never sent back - it is only written.")
public class SmtpController {

	@Inject
	private SmtpService smtpService;

	@POST
	@Path(ApiRoute.smtpTable)
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(operationId = "getSmtpTable", summary = "One page of the mail servers", description = "Returns one page of the mail servers, in the same shape as every other table of the application: the rows, the columns with their translated names, the whole number of rows and what the table itself can do. The page, the filters and the sorting are sent in the body; without a sort the servers come by name. The password is not among the values of a row.", requestBody = @RequestBody(description = "Page, filters and sorting", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseParameter.class))), responses = {
			@ApiResponse(responseCode = "200", description = "One page of the mail servers", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DatabaseTable.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user is not an administrator", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getTable(DatabaseParameter databaseParameter) {
		return Response.ok(smtpService.getTable(databaseParameter)).build();
	}

	@GET
	@Path(ApiRoute.smtpId)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(operationId = "getSmtp", summary = "One mail server", description = "Returns one mail server by its identifier, as the form of the entry asks for it. The password is sent back empty, whether one is stored or not: it leaves the application only towards the mail server itself.", responses = {
			@ApiResponse(responseCode = "200", description = "The mail server, without its password", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SmtpDTO.class))),
			@ApiResponse(responseCode = "400", description = "No mail server with the given identifier", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user is not an administrator", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getSmtp(
			@Parameter(description = "Identifier of the mail server", required = true) @PathParam("id") UUID id) {
		return Response.ok(smtpService.getSmtp(id)).build();
	}

	@POST
	@Path(ApiRoute.smtp)
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(operationId = "updateSmtp", summary = "Add or change a mail server", description = "Writes a mail server: without an identifier it is added, with one the server of that identifier is changed. Two servers cannot carry the same name. When it signs in, the username is required, and so is the password for a new server; on a change an empty password keeps the one already stored. When it does not sign in, the username and the password are cleared. The answer is the server as it stands, without its password.", requestBody = @RequestBody(description = "The mail server; the identifier is empty for a new one", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SmtpDTO.class))), responses = {
			@ApiResponse(responseCode = "200", description = "The mail server as it stands after the write, without its password", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SmtpDTO.class))),
			@ApiResponse(responseCode = "400", description = "A required value is missing, the name is taken, or there is no server with that identifier", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user is not an administrator", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getUpdate(@Valid SmtpDTO smtpDTO) {
		return Response.ok(smtpService.getUpdate(smtpDTO)).build();
	}

	@DELETE
	@Path(ApiRoute.smtpId)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(operationId = "deleteSmtp", summary = "Delete a mail server", description = "Deletes the mail server of the given identifier. Nothing is returned.", responses = {
			@ApiResponse(responseCode = "204", description = "The mail server has been deleted"),
			@ApiResponse(responseCode = "400", description = "No mail server with the given identifier", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user is not an administrator", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getDelete(
			@Parameter(description = "Identifier of the mail server", required = true) @PathParam("id") UUID id) {
		smtpService.getDelete(id);
		return Response.noContent().build();
	}

	@POST
	@Path(ApiRoute.smtpTestMail)
	@Consumes(MediaType.APPLICATION_JSON)
	@RolesAllowed(RoleList.ADMIN)
	@Operation(operationId = "sendTestMail", summary = "Send a test mail", description = "Sends one mail through the given mail server, to see whether the server really works. The subject is the one written in the settings of the application, and the address it goes to is sent in the body. Nothing is returned when the mail has left; when the mail server refuses it, the answer carries what the server said. Every try is written down, whether it worked or not.", requestBody = @RequestBody(description = "Address the test mail is sent to", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TestMailDTO.class))), responses = {
			@ApiResponse(responseCode = "204", description = "The test mail has been sent"),
			@ApiResponse(responseCode = "400", description = "No address was given, there is no mail server with that identifier, or the mail server did not take the mail", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "401", description = "No valid session", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "403", description = "Current user is not an administrator", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getSendTestMail(@Valid TestMailDTO testMailDTO,
			@Parameter(description = "Identifier of the mail server the test mail goes through", required = true) @PathParam("smtpId") UUID smtpId) {
		smtpService.getSendTestMail(testMailDTO, smtpId);
		return Response.noContent().build();
	}
}
