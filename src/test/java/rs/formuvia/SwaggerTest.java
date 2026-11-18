package rs.formuvia;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

/**
 * Every endpoint is described in /api/openapi.json, the way AppUserController shows: a tag on the controller, a
 * summary and a description of the operation, the answers it gives and every value of the path explained.
 */
class SwaggerTest {

	private static final List<Class<? extends Annotation>> HTTP_METHODS = List.of(GET.class, POST.class, PUT.class,
			DELETE.class);

	private static List<Class<?>> controllers;
	private static List<Method> endpoints = new ArrayList<>();

	@BeforeAll
	static void find() {
		controllers = new Reflections(Formuvia.PACKAGE_NAME).getTypesAnnotatedWith(Path.class).stream()
				.filter(c -> c.getSimpleName().endsWith("Controller"))
				.sorted((a, b) -> a.getName().compareTo(b.getName())).<Class<?>>map(c -> c).toList();
		endpoints.clear();
		for (Class<?> controller : controllers)
			for (Method method : controller.getDeclaredMethods())
				if (HTTP_METHODS.stream().anyMatch(method::isAnnotationPresent))
					endpoints.add(method);
		endpoints.sort((a, b) -> name(a).compareTo(name(b)));
	}

	private static String name(Method method) {
		return method.getDeclaringClass().getSimpleName() + "." + method.getName();
	}

	private static void assertNone(List<String> wrong, String what) {
		assertTrue(wrong.isEmpty(), what + ": " + wrong);
	}

	@Test
	void controllersAndEndpointsAreFound() {
		assertTrue(controllers.size() > 10, "controllers: " + controllers.size());
		assertTrue(endpoints.size() > 50, "endpoints: " + endpoints.size());
	}

	@Test
	void everyControllerHasATagWithADescription() {
		assertNone(controllers.stream().filter(c -> {
			Tag tag = c.getAnnotation(Tag.class);
			return tag == null || tag.name().isBlank() || tag.description().isBlank();
		}).map(Class::getSimpleName).toList(), "Controllers without @Tag(name, description)");
	}

	@Test
	void everyEndpointHasASummaryAndADescription() {
		assertNone(endpoints.stream().filter(m -> {
			Operation operation = m.getAnnotation(Operation.class);
			return operation == null || operation.summary().isBlank() || operation.description().isBlank();
		}).map(SwaggerTest::name).toList(), "Endpoints without @Operation(summary, description)");
	}

	private static Set<String> responseCodes(Method method) {
		Operation operation = method.getAnnotation(Operation.class);
		return operation == null ? Set.of()
				: Arrays.stream(operation.responses()).map(ApiResponse::responseCode).collect(Collectors.toSet());
	}

	@Test
	void everyEndpointSaysWhatItAnswersWhenItWorks() {
		assertNone(endpoints.stream().filter(m -> responseCodes(m).stream().noneMatch(code -> code.startsWith("2")))
				.map(SwaggerTest::name).toList(), "Endpoints without a 2xx response");
	}

	@Test
	void everyEndpointThatNeedsARoleSaysSo() {
		// without a session the answer is 401, without the role 403
		assertNone(endpoints.stream().filter(m -> m.isAnnotationPresent(RolesAllowed.class))
				.filter(m -> !responseCodes(m).containsAll(Set.of("401", "403"))).map(SwaggerTest::name).toList(),
				"Endpoints with @RolesAllowed but without the 401 and 403 responses");
	}

	@Test
	void everyValueOfThePathIsExplained() {
		List<String> wrong = new ArrayList<>();
		for (Method method : endpoints)
			for (Parameter parameter : method.getParameters())
				if (parameter.isAnnotationPresent(PathParam.class)) {
					io.swagger.v3.oas.annotations.Parameter described = parameter
							.getAnnotation(io.swagger.v3.oas.annotations.Parameter.class);
					if (described == null || described.description().isBlank())
						wrong.add(name(method) + "(" + parameter.getAnnotation(PathParam.class).value() + ")");
				}
		assertNone(wrong, "Path values without @Parameter(description)");
	}
}
