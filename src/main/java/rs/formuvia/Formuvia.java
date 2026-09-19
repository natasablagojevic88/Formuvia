package rs.formuvia;

import java.util.Set;

import org.glassfish.jersey.server.ResourceConfig;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Context;
import rs.formuvia.utils.CustomAbstractBinder;
import rs.formuvia.utils.CustomServletContextListener;

@ApplicationPath("/api")
public class Formuvia extends ResourceConfig {

	public static final String PACKAGE_NAME = "rs.formuvia";
	private final String SWAGGER_TITLE = "Formuvia";
	private final String SWAGGER_VERSION = "1.0";

	@Context
	private ServletContext servletContext;

	public Formuvia() {
		packages(PACKAGE_NAME);
		register(loadSwagger());
		register(new CustomAbstractBinder());
	}

	private OpenApiResource loadSwagger() {
		OpenApiResource openApiResource = new OpenApiResource();

		OpenAPI openAPI = new OpenAPI();
		Info info = new Info();
		info.title(SWAGGER_TITLE).version(SWAGGER_VERSION);
		openAPI.info(info);

		Server server = new Server();
		server.setUrl(CustomServletContextListener.context);
		openAPI.addServersItem(server);

		SwaggerConfiguration swaggerConfiguration = new SwaggerConfiguration();
		swaggerConfiguration.openAPI(openAPI);
		swaggerConfiguration.prettyPrint(true);
		swaggerConfiguration.resourcePackages(Set.of(PACKAGE_NAME));

		openApiResource.openApiConfiguration(swaggerConfiguration);
		return openApiResource;
	}
}
