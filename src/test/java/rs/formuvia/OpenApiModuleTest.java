package rs.formuvia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.core.util.Json;
import jakarta.ws.rs.ApplicationPath;
import rs.formuvia.utils.CustomServletContextListener;
import rs.formuvia.utils.OpenApiModuleResource;
import rs.formuvia.utils.StaticData;

/**
 * Swagger UI offers one definition per module next to the whole application (SwaggerServlet), and "Try it out"
 * calls the server of the definition plus the path of the endpoint. Both together must lead under /api, where
 * Jersey serves the endpoints, or every call from a module ends in 404.
 */
class OpenApiModuleTest {

	private static final String CONTEXT = "/Formuvia";
	private static final String API = Formuvia.class.getAnnotation(ApplicationPath.class).value();

	private static List<Class<?>> classesBefore;
	private static String contextBefore;

	@BeforeAll
	static void scan() {
		// the same scan as AppStartUpImpl.loadClass, which fills the list the modules are read from
		classesBefore = StaticData.allClasses;
		contextBefore = CustomServletContextListener.context;
		StaticData.allClasses = new Reflections(Formuvia.PACKAGE_NAME, Scanners.SubTypes.filterResultsBy(_ -> true))
				.getSubTypesOf(Object.class).stream().filter(a -> a.getEnclosingClass() == null)
				.filter(a -> !a.isSynthetic()).toList();
		CustomServletContextListener.context = CONTEXT;
	}

	@AfterAll
	static void restore() {
		StaticData.allClasses = classesBefore;
		CustomServletContextListener.context = contextBefore;
	}

	private static JsonNode document(String module) throws Exception {
		Object entity = new OpenApiModuleResource().getModule(module).getEntity();
		return Json.mapper().readTree(entity.toString());
	}

	@Test
	void modulesAreFound() {
		List<String> modules = OpenApiModuleResource.modules();
		assertTrue(modules.contains("administration"), modules.toString());
		assertTrue(modules.contains("model"), modules.toString());
	}

	@Test
	void everyEndpointOfAModuleIsCalledUnderApi() throws Exception {
		List<String> wrong = new ArrayList<>();
		for (String module : OpenApiModuleResource.modules()) {
			JsonNode document = document(module);
			JsonNode servers = document.path("servers");
			assertEquals(1, servers.size(), module + " servers: " + servers);
			String server = servers.get(0).path("url").asText();

			List<String> paths = new ArrayList<>();
			document.path("paths").fieldNames().forEachRemaining(paths::add);
			assertFalse(paths.isEmpty(), module + " has no endpoints");

			for (String path : paths) {
				String url = server + path;
				if (!url.startsWith(CONTEXT + API + "/") || url.contains(API + API + "/"))
					wrong.add(module + ": " + url);
			}
		}
		assertTrue(wrong.isEmpty(), "Calls from Swagger UI that miss " + API + ": " + wrong);
	}
}
