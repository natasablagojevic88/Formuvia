package rs.formuvia.utils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import rs.formuvia.Formuvia;

@Hidden
@Path("")
public class OpenApiModuleResource {

	public static final String MODULE_PARAMETER = "module";
	private static final String CONTROLLER_PACKAGE = ".controller";

	private static final Map<String, String> documents = new ConcurrentHashMap<>();

	/** Modules that hold endpoints, read from the packages of the controllers. */
	public static List<String> modules() {
		return StaticData.allClasses.stream().filter(a -> a.isAnnotationPresent(Path.class))
				.map(a -> a.getPackageName()).filter(a -> a.startsWith(Formuvia.PACKAGE_NAME + "."))
				.filter(a -> a.endsWith(CONTROLLER_PACKAGE))
				.map(a -> a.substring(Formuvia.PACKAGE_NAME.length() + 1, a.length() - CONTROLLER_PACKAGE.length()))
				.distinct().sorted().toList();
	}

	/** Name of the module as the definition is offered in Swagger UI. */
	public static String title(String module) {
		return Character.toUpperCase(module.charAt(0)) + module.substring(1);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ApiRoute.openApiModule)
	public Response getModule(@QueryParam(MODULE_PARAMETER) String module) {
		if (!modules().contains(module))
			throw new WebApplicationException(Response.Status.NOT_FOUND);

		return Response.ok(documents.computeIfAbsent(module, OpenApiModuleResource::createDocument)).build();
	}

	private static String createDocument(String module) {
		try {
			OpenApiContext openApiContext = new JaxrsOpenApiContextBuilder<>()
					.openApiConfiguration(
							Formuvia.createSwaggerConfiguration(Formuvia.PACKAGE_NAME + "." + module))
					.ctxId(module).buildContext(true);

			return openApiContext.getOutputJsonMapper().writerWithDefaultPrettyPrinter()
					.writeValueAsString(openApiContext.read());
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
}
