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
@Tag(name = "Authentication", description = "Signing users in and out. A successful login issues an HttpOnly access token cookie that authenticates all subsequent requests.")
public class LoginController {
	
	@Inject
	private LoginService loginService;
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.login)
	@Operation(
			summary = "Log in",
			description = "Verifies the username and password of an active user. On success a new session is created and the access token is returned in an HttpOnly cookie. The response body is empty.",
			requestBody = @RequestBody(
					description = "User credentials",
					required = true,
					content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LoginDTO.class))),
			responses = {
					@ApiResponse(responseCode = "204", description = "Login successful, access token cookie is set"),
					@ApiResponse(responseCode = "400", description = "Wrong username or password, inactive user, or a required field is missing",
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
			description = "Deactivates the current session and clears the access token cookie. Requires a valid session.",
			responses = {
					@ApiResponse(responseCode = "204", description = "Logged out, access token cookie is cleared"),
					@ApiResponse(responseCode = "401", description = "No valid session, access token cookie is cleared",
							content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getLogout(
			){
		this.loginService.logout();
		return Response.noContent().build();
	}
}
