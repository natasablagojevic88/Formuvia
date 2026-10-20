package rs.formuvia.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/swagger-ui/*")
public class SwaggerServlet extends HttpServlet {

	private static final String WEBJAR_ROOT = "/webjars/swagger-ui/5.32.14";
	private static final String INITIALIZER = "/swagger-initializer.js";
	private static final String OPENAPI_URL = "api/openapi.json";
	private static final String OPENAPI_MODULE_URL = "api" + ApiRoute.openApiModule + "?"
			+ OpenApiModuleResource.MODULE_PARAMETER + "=";
	private static final String ALL_MODULES = "All";
	private static final String SWAGGER_URL_REPLACE = "url: \"https://petstore.swagger.io/v2/swagger.json\",";
	private static final String SWAGGER_OPTIONS_REPLACE = "deepLinking: true,";
	private static final String SWAGGER_OPTIONS = "deepLinking: true,\n" + "    docExpansion: \"none\",\n"
			+ "    tagsSorter: \"alpha\",\n" + "    operationsSorter: \"alpha\",";

	private static final long serialVersionUID = 1L;

	public SwaggerServlet() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String file = request.getPathInfo();

		List<String> parts = Arrays.asList(request.getRequestURL().toString().split("\\/"));

		if (INITIALIZER.equals(file)) {
			String urlPath = parts.stream().limit(parts.size() - 2).map(a -> a + "/").collect(Collectors.joining());

			String bodyResponse = new String(getServletContext().getResourceAsStream(WEBJAR_ROOT + file).readAllBytes(),
					StandardCharsets.UTF_8);

			response.setContentType(getServletContext().getMimeType(file));
			bodyResponse = bodyResponse.replace(SWAGGER_URL_REPLACE, createUrls(urlPath));
			bodyResponse = bodyResponse.replace(SWAGGER_OPTIONS_REPLACE, SWAGGER_OPTIONS);
			try (InputStream in = new ByteArrayInputStream(bodyResponse.getBytes(StandardCharsets.UTF_8))) {
				in.transferTo(response.getOutputStream());
			}

		} else {
			response.setContentType(getServletContext().getMimeType(file));

			try (InputStream in = getServletContext().getResourceAsStream(WEBJAR_ROOT + file)) {
				in.transferTo(response.getOutputStream());
			}
		}

	}

	/** The picker of definitions: the whole application, and one definition per module. */
	private String createUrls(String urlPath) {
		StringBuilder urls = new StringBuilder("urls: [\n");
		urls.append(createUrl(urlPath + OPENAPI_URL, ALL_MODULES));

		for (String module : OpenApiModuleResource.modules())
			urls.append(",\n")
					.append(createUrl(urlPath + OPENAPI_MODULE_URL + module, OpenApiModuleResource.title(module)));

		urls.append("\n    ],\n    \"urls.primaryName\": \"").append(ALL_MODULES).append("\",");
		return urls.toString();
	}

	private String createUrl(String url, String name) {
		return "      { url: \"" + url + "\", name: \"" + name + "\" }";
	}

}
