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
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import rs.formuvia.common.dto.LoginDTO;
import rs.formuvia.common.service.LoginService;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.ErrorDetail;

@Path("")
@Tag(name = "Authentication", description = "Signing in and out. Sessions are cookie based: a successful login sets an HttpOnly access token cookie that the browser sends automatically with every following request.")
public class LoginController {
	
	@Inject
	private LoginService loginService;
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.login)
	@Operation(
			summary = "Log in",
			description = "Checks the username and password of an active user and starts a new session. The access token is returned in a cookie (name from cookie.access.token.name, default access_token) that lives as long as the session (cookie.refresh.token.duration.minutes). The token itself is valid for cookie.access.token.duration.minutes and is renewed automatically on the next request while the session is still valid. Does not require a session. The response body is empty.",
			requestBody = @RequestBody(
					description = "Username and password",
					required = true,
					content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LoginDTO.class))),
			responses = {
					@ApiResponse(responseCode = "204", description = "Login successful, the access token cookie is set"),
					@ApiResponse(responseCode = "400", description = "Wrong username or password, the user is inactive (same message in both cases), or username or password is missing",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getLogin(
			@Valid LoginDTO loginDTO 
			){
		this.loginService.login(loginDTO);
		return Response.noContent().build();
	}
	
	@POST
	@Path(ApiRoute.loginLogout)
	@Operation(
			summary = "Log out",
			description = "Ends the current session: the session is marked inactive, so its access token can no longer be used, and the access token cookie is removed. Requires a valid session. The response body is empty.",
			responses = {
					@ApiResponse(responseCode = "204", description = "Logged out, the access token cookie is removed"),
					@ApiResponse(responseCode = "401", description = "No valid session, the access token cookie is removed",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getLogout(
			){
		this.loginService.logout();
		return Response.noContent().build();
	}
}
