package rs.formuvia.common.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
import rs.formuvia.common.service.SessionService;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.ErrorDetail;

@Path("")
@Tag(name = "Session", description = "Checking the state of the current user session.")
public class SessionController {

	@Inject
	private SessionService sessionService;

	@GET
	@Path(ApiRoute.session)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Check session", description = "Checks whether the access token cookie belongs to a valid, active session. If the access token has expired but the refresh period is still valid, a new access token cookie is issued. Intended for the client to verify the session on startup and to keep it alive.", responses = {
			@ApiResponse(responseCode = "200", description = "Session is valid, the access token cookie may have been renewed"),
			@ApiResponse(responseCode = "401", description = "Session is missing, expired or inactive, access token cookie is cleared", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getSession() {
		return Response.ok(sessionService.getUserInfo()).build();
	}

	@POST
	@Path(ApiRoute.sessionChangePassword)
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getChangePassword(@Valid ChangePasswordDTO changePasswordDTO) {
		this.sessionService.changePassword(changePasswordDTO);
		return Response.noContent().build();
	}
}
