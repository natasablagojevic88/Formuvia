package rs.formuvia;

import java.util.Set;

import org.glassfish.jersey.server.ResourceConfig;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.jackson.TypeNameResolver;
import io.swagger.v3.core.util.Json;
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
import rs.formuvia.utils.OpenApiModuleResource;

@ApplicationPath("/api")
public class Formuvia extends ResourceConfig {

	public static final String PACKAGE_NAME = "rs.formuvia";
	private static final String SWAGGER_TITLE = "Formuvia";
	private static final String SWAGGER_VERSION = "1.0";

	@Context
	private ServletContext servletContext;

	public Formuvia() {
		packages(PACKAGE_NAME);
		register(loadSwagger());
		register(new CustomAbstractBinder());
	}

	private OpenApiResource loadSwagger() {
		OpenApiResource openApiResource = new OpenApiResource();

		ModelConverters.getInstance().addConverter(new ModelResolver(Json.mapper(), new ModuleTypeNameResolver()));

		openApiResource.openApiConfiguration(createSwaggerConfiguration(PACKAGE_NAME));
		return openApiResource;
	}

	/** One document: the whole application for the root package, one module for a module package. */
	public static SwaggerConfiguration createSwaggerConfiguration(String resourcePackage) {
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
		swaggerConfiguration.sortOutput(true);
		swaggerConfiguration.resourcePackages(Set.of(resourcePackage));

		return swaggerConfiguration;
	}

	/** Schema name carries the module it comes from, so sorted output groups the definitions by package. */
	private static class ModuleTypeNameResolver extends TypeNameResolver {

		@Override
		protected String nameForClass(Class<?> cls, Set<Options> options) {
			String name = super.nameForClass(cls, options);
			String packageName = cls.getPackageName();

			if (!packageName.startsWith(PACKAGE_NAME + "."))
				return name;

			String module = packageName.substring(PACKAGE_NAME.length() + 1);
			if (module.contains("."))
				module = module.substring(0, module.indexOf('.'));

			return OpenApiModuleResource.title(module) + "." + name;
		}
	}
}
