package rs.formuvia.common.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import rs.formuvia.common.dto.ChangePasswordDTO;
import rs.formuvia.common.dto.UserInfoDTO;
import rs.formuvia.common.service.SessionService;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.ErrorDetail;

@Path("")
@Tag(name = "Session", description = "The signed-in user: their data, their menu and changing their own password. All endpoints require a valid session.")
public class SessionController {

	@Inject
	private SessionService sessionService;

	@GET
	@Path(ApiRoute.session)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(
			summary = "Get current user",
			description = "Returns the username, first name and last name of the signed-in user together with the application menu. The menu contains only the items allowed for the user's roles, with names translated to the language from the X-Language header. The client calls it on startup to find out whether the user is signed in.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Signed-in user and their menu",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UserInfoDTO.class))),
					@ApiResponse(responseCode = "401", description = "No valid session, the access token cookie is removed",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getSession() {
		return Response.ok(sessionService.getUserInfo()).build();
	}

	@POST
	@Path(ApiRoute.sessionChangePassword)
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(
			summary = "Change own password",
			description = "Sets a new password for the signed-in user. The new password is sent twice and both values must match; the current password is not required. The password is stored as a bcrypt hash. The current session stays active. The response body is empty.",
			requestBody = @RequestBody(
					description = "New password and its confirmation",
					required = true,
					content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ChangePasswordDTO.class))),
			responses = {
					@ApiResponse(responseCode = "204", description = "Password changed"),
					@ApiResponse(responseCode = "400", description = "A field is missing or the new password and its confirmation do not match",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
					@ApiResponse(responseCode = "401", description = "No valid session, the access token cookie is removed",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getChangePassword(@Valid ChangePasswordDTO changePasswordDTO) {
		this.sessionService.changePassword(changePasswordDTO);
		return Response.noContent().build();
	}
}
