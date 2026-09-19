package rs.formuvia.common.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import rs.formuvia.common.service.ResourceBundleService;
import rs.formuvia.utils.ApiRoute;

@Path("")
@Tag(name = "Translations", description = "Texts of the user interface in the language selected by the X-Language header.")
public class TranslationController {

	@Inject
	private ResourceBundleService resourceBundleService;

	@GET
	@Path(ApiRoute.translations)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(
			summary = "Get translations",
			description = "Returns every translated text of the application (messages, labels, menu and interface texts) as a map of translation keys to texts, in the language from the X-Language header. Does not require a session, so it can also be used on the login page.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Map of translation keys to texts") })
	public Response getTranslations() {
		return Response.ok(resourceBundleService.getAllTexts()).build();
	}
}
