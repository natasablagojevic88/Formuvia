package rs.formuvia.common.controller;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import rs.formuvia.common.service.ExecuteWebhookService;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.ErrorDetail;

@Path("")
@Tag(name = "Webhook", description = "Running a job of the schedule from outside the application. This is the only endpoint that is not reached with a session: the caller proves itself with the key of the webhook, in the Authorization header. The keys are made under 'Webhook keys'.")
public class ExecuteWebhookController {

	@Inject
	private ExecuteWebhookService executeWebhookService;

	@Path(ApiRoute.webhookExecute)
	@POST
	@Operation(summary = "Run a job by its webhook key", description = "Runs the job the key belongs to, right away, and does the same thing the schedule does: the report is made and sent, written into a file, or only run. The identifier in the path is the identifier of the key, not of the job, so every key has an address of its own and a key that is taken away stops working. The request carries no body; the key goes in the header, as 'Authorization: Bearer <key>'. The answer comes back when the job is done, and nothing is returned. No session and no cookie are needed.", responses = {
			@ApiResponse(responseCode = "204", description = "The job went through"),
			@ApiResponse(responseCode = "401", description = "The Authorization header is missing, is not a Bearer header, or the key is not the key of this webhook", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))),
			@ApiResponse(responseCode = "400", description = "No webhook with that identifier, or the job did not go through", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDetail.class))) })
	public Response getExecuteWebhook(
			@Parameter(description = "Identifier of the webhook key, as it stands in the address the user copied", required = true) @PathParam("id") UUID id) {
		executeWebhookService.getExecute(id);
		return Response.noContent().build();
	}
}
