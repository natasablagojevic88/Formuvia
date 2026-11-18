package rs.formuvia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import rs.formuvia.model.utils.UpdateColumnModel;
import rs.formuvia.model.utils.UpdateModel;
import rs.formuvia.utils.ApiRoute;

/**
 * Some things are written on both sides and must stay the same: the routes of the API, the rules for the code
 * of a table and of a column, and the pages the menu opens. The front is looked for beside this project
 * (../FormuviaFront, as in build.xml); where it is not there, as on a server that builds the back alone,
 * these tests are skipped.
 */
class FrontSyncTest {

	static final Path FRONT = Path.of("../FormuviaFront/src/app");

	@BeforeAll
	static void frontIsHere() {
		assumeTrue(Files.isDirectory(FRONT), "FormuviaFront is not beside the back; the front is not compared");
	}

	private static String front(String file) throws IOException {
		return Files.readString(FRONT.resolve(file));
	}

	/** "/x/{id}/y" and "/x/${id}/y" both become "/x/{}/y". */
	private static String shape(String path) {
		return path.replaceAll("\\$?\\{[^}]*\\}", "{}");
	}

	@Test
	void everyRouteTheFrontCallsExistsOnTheBack() throws IOException, IllegalAccessException {
		Set<String> back = new TreeSet<>();
		for (Field field : ApiRoute.class.getFields())
			if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class)
				back.add(shape((String) field.get(null)));

		Set<String> front = new TreeSet<>();
		Matcher matcher = Pattern.compile("['`](/api/[^'`]*)['`]").matcher(front("shared/ApiRoute.ts"));
		while (matcher.find())
			front.add(shape(matcher.group(1).substring("/api".length())));
		assertTrue(front.size() > 30, "the routes of the front were not found: " + front.size());

		Set<String> missing = new TreeSet<>(front);
		missing.removeAll(back);
		assertTrue(missing.isEmpty(), "ApiRoute.ts calls routes that ApiRoute.java does not have: " + missing);
	}

	/** The source of a JavaScript regular expression: export const NAME = /source/; */
	private static String jsPattern(String file, String name) throws IOException {
		Matcher matcher = Pattern.compile("export const " + name + "\\s*=\\s*/(.+)/;").matcher(front(file));
		assertTrue(matcher.find(), name + " was not found in " + file);
		return matcher.group(1);
	}

	@Test
	void codeOfAColumnFollowsTheSameRule() throws IOException {
		assertEquals(UpdateColumnModel.COLUMN_NAME.pattern(), jsPattern("model/model-column.ts", "COLUMN_CODE_PATTERN"),
				"UpdateColumnModel.COLUMN_NAME and COLUMN_CODE_PATTERN in model-column.ts differ");
	}

	@Test
	void codeOfATableFollowsTheSameRule() throws Exception {
		Field field = UpdateModel.class.getDeclaredField("NAME_PARENT");
		field.setAccessible(true);
		String back = ((Pattern) field.get(null)).pattern();

		assertEquals(back, jsPattern("model/model.ts", "TABLE_CODE_PATTERN"),
				"UpdateModel.NAME_PARENT and TABLE_CODE_PATTERN in model.ts differ");
	}

	@Test
	void everyPageOfTheMenuIsARouteOfTheFront() throws Exception {
		MenuTest.read();
		String routing = front("app-routing-module.ts");
		Set<String> routes = new TreeSet<>();
		Matcher matcher = Pattern.compile("path:\\s*'([^']*)'").matcher(routing);
		while (matcher.find())
			routes.add(matcher.group(1));

		List<String> missing = MenuTest.urls().stream()
				.filter(url -> !routes.contains(url.substring(1).split("/")[0])).toList();
		assertTrue(missing.isEmpty(), "menu.xml opens pages the front does not have: " + missing);
	}
}
