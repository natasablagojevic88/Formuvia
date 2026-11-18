package rs.formuvia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.reflections.Reflections;

import rs.formuvia.common.enums.TrackAction;
import rs.formuvia.database.annotations.EntityClass;

/**
 * A key that is missing in a bundle is shown to the user as it is ("wrongType" instead of a sentence), and a key
 * that is missing in one language only is noticed by nobody until someone switches the language. So every text
 * the server sends must be in all three languages: en_US, sr_Latn_RS and sr_RS.
 */
class ResourceBundleTest {

	static final Path RESOURCES = Path.of("resources");
	static final Path SOURCES = Path.of("src/main/java");
	static final List<String> LOCALES = List.of("en_US", "sr_Latn_RS", "sr_RS");
	/** The same groups, in the same order, as ResourceBundleServiceImpl.RESOURCE_BUNDLE_PATH. */
	static final List<String> GROUPS = List.of("error", "dto", "menu", "common", "enum");

	/** locale -> every key of every group, the way ResourceBundleServiceImpl.getText looks for it. */
	private static final Map<String, Set<String>> allKeys = new TreeMap<>();

	static Properties load(String group, String locale) throws IOException {
		Properties properties = new Properties();
		try (InputStream in = Files.newInputStream(RESOURCES.resolve("resource_bundle_" + group + "_" + locale + ".properties"))) {
			properties.load(in);
		}
		return properties;
	}

	@BeforeAll
	static void loadAll() throws IOException {
		for (String locale : LOCALES) {
			Set<String> keys = new TreeSet<>();
			for (String group : GROUPS)
				keys.addAll(load(group, locale).stringPropertyNames());
			allKeys.put(locale, keys);
		}
	}

	private static void assertTranslated(Set<String> keys, String what) {
		for (String locale : LOCALES) {
			Set<String> missing = new TreeSet<>(keys);
			missing.removeAll(allKeys.get(locale));
			assertTrue(missing.isEmpty(), what + " without a translation in " + locale + ": " + missing);
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "error", "dto", "menu", "common", "enum" })
	void everyLanguageHasTheSameKeys(String group) throws IOException {
		Set<String> english = load(group, "en_US").stringPropertyNames();
		for (String locale : LOCALES) {
			Set<String> keys = load(group, locale).stringPropertyNames();
			Set<String> missing = new TreeSet<>(english);
			missing.removeAll(keys);
			Set<String> extra = new TreeSet<>(keys);
			extra.removeAll(english);
			assertTrue(missing.isEmpty() && extra.isEmpty(),
					group + "_" + locale + " - missing: " + missing + ", not in en_US: " + extra);
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "error", "dto", "menu", "common", "enum" })
	void noTextIsEmpty(String group) throws IOException {
		for (String locale : LOCALES) {
			Properties properties = load(group, locale);
			List<String> empty = properties.stringPropertyNames().stream()
					.filter(key -> properties.getProperty(key).isBlank()).sorted().toList();
			assertTrue(empty.isEmpty(), group + "_" + locale + " has empty texts: " + empty);
		}
	}

	@Test
	void filesAreAsciiWithUnicodeEscapes() throws IOException {
		// Properties are read as ISO-8859-1: a "č" or "ж" written as it is would come out broken
		try (Stream<Path> files = Files.list(RESOURCES)) {
			for (Path file : files.filter(f -> f.getFileName().toString().startsWith("resource_bundle_")).toList()) {
				byte[] bytes = Files.readAllBytes(file);
				for (int i = 0; i < bytes.length; i++)
					assertTrue(bytes[i] >= 0, file.getFileName() + ": a character that is not ASCII at byte " + i
							+ "; write it as \\uXXXX");
			}
		}
	}

	@Test
	void everyErrorKeyThrownInTheCodeIsTranslated() throws IOException {
		Set<String> keys = errorKeysInCode();
		assertTrue(keys.size() > 50, "the keys were not found in the code: " + keys.size());
		assertTranslated(keys, "Error keys");
	}

	/**
	 * Keys given as text where the code throws or translates: new CommonException(status, "key", ...),
	 * super("key") in the exceptions, getText("key"); a key given as a constant (KEY or Class.KEY) is
	 * looked up among the String constants of the code. A message put together with "+" is not a key: ReportJob
	 * writes it into the log of the job and into the error mail as it is, without the exception mapper.
	 */
	static Set<String> errorKeysInCode() throws IOException {
		Pattern common = Pattern.compile(
				"new CommonException\\([^,]+,\\s*(\"([^\"]+)\"\\s*[,)]|([A-Za-z_][A-Za-z0-9_]*)\\s*[,)])");
		Pattern superKey = Pattern.compile("super\\(\"([^\"]+)\"\\)");
		Pattern getText = Pattern.compile("getText\\(\"([^\"]+)\"\\)");
		Pattern getTextConstant = Pattern.compile("getText\\((?:(\\w+)\\.)?([A-Z][A-Z0-9_]*)\\)");
		Pattern constantDeclaration = Pattern.compile("static final String ([A-Z][A-Z0-9_]*)\\s*=\\s*\"([^\"]+)\"");
		Map<String, String> sources = new HashMap<>();
		try (Stream<Path> files = Files.walk(SOURCES)) {
			for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList())
				sources.put(file.toString(), Files.readString(file, StandardCharsets.UTF_8));
		}
		// "Class.KEY" -> value, and "path/of/File.java#KEY" -> value for a constant used in its own class
		Map<String, String> constants = new HashMap<>();
		for (Map.Entry<String, String> entry : sources.entrySet()) {
			String className = Path.of(entry.getKey()).getFileName().toString().replace(".java", "");
			Matcher matcher = constantDeclaration.matcher(entry.getValue());
			while (matcher.find()) {
				constants.put(className + "." + matcher.group(1), matcher.group(2));
				constants.put(entry.getKey() + "#" + matcher.group(1), matcher.group(2));
			}
		}
		Set<String> keys = new TreeSet<>();
		{
			for (Map.Entry<String, String> entry : sources.entrySet()) {
				Path file = Path.of(entry.getKey());
				String source = entry.getValue();
				Matcher matcher = common.matcher(source);
				while (matcher.find()) {
					if (matcher.group(2) != null) {
						keys.add(matcher.group(2));
					} else {
						Matcher constant = Pattern.compile("\\b" + matcher.group(3) + "\\s*=\\s*\"([^\"]+)\"")
								.matcher(source);
						if (constant.find())
							keys.add(constant.group(1));
					}
				}
				if (file.toString().contains("exceptions")) {
					matcher = superKey.matcher(source);
					while (matcher.find())
						keys.add(matcher.group(1));
				}
				matcher = getText.matcher(source);
				while (matcher.find())
					keys.add(matcher.group(1));
				matcher = getTextConstant.matcher(source);
				while (matcher.find()) {
					String value = matcher.group(1) == null ? constants.get(entry.getKey() + "#" + matcher.group(2))
							: constants.get(matcher.group(1) + "." + matcher.group(2));
					if (value != null)
						keys.add(value);
				}
			}
		}
		return keys;
	}

	/**
	 * The DTOs a table is made of: those given to databaseService.createTable(..., X.class). Only for them
	 * CreateDatabaseTable sends the translated title, the labels (allColumns) and the enum values.
	 */
	static List<Class<?>> tableDtos() throws IOException {
		Set<String> names = new TreeSet<>();
		Pattern createTable = Pattern.compile("createTable\\([^;]*?,\\s*(\\w+)\\.class");
		try (Stream<Path> files = Files.walk(SOURCES)) {
			for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
				Matcher matcher = createTable.matcher(Files.readString(file, StandardCharsets.UTF_8));
				while (matcher.find())
					names.add(matcher.group(1));
			}
		}
		List<Class<?>> dtos = new Reflections(Formuvia.PACKAGE_NAME).getTypesAnnotatedWith(EntityClass.class)
				.stream().filter(c -> names.contains(c.getSimpleName()))
				.sorted((a, b) -> a.getName().compareTo(b.getName())).<Class<?>>map(c -> c).toList();
		assertEquals(names.size(), dtos.size(), "a DTO given to createTable has no @EntityClass: " + names);
		return dtos;
	}

	@Test
	void everyTableHasATitleAndEveryFieldALabel() throws IOException {
		// CreateDatabaseTable: "<Dto>.title" is the name of the table, "<Dto>.<field>" the label of a column
		// and of the field in the form (allColumns)
		List<Class<?>> dtos = tableDtos();
		assertTrue(dtos.size() >= 5, "the DTOs were not found: " + dtos.size());
		Set<String> keys = new TreeSet<>();
		for (Class<?> dto : dtos) {
			keys.add(dto.getSimpleName() + ".title");
			for (Field field : dto.getDeclaredFields())
				if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic())
					keys.add(dto.getSimpleName() + "." + field.getName());
		}
		assertTranslated(keys, "Table titles and field labels");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	void everyValueOfAnEnumShownInATableIsTranslated() throws IOException {
		// CreateDatabaseTable fills listOfValues with "<Enum>.<VALUE>"
		Set<String> keys = new TreeSet<>();
		List<String> enums = new ArrayList<>();
		for (Class<?> dto : tableDtos())
			for (Field field : dto.getDeclaredFields())
				if (field.getType().isEnum()) {
					Class<? extends Enum> type = (Class<? extends Enum>) field.getType();
					enums.add(type.getSimpleName());
					for (Enum value : type.getEnumConstants())
						keys.add(type.getSimpleName() + "." + value.name());
				}
		assertTrue(!enums.isEmpty(), "no enum found in the DTOs");
		// the history of a record (CreateHistory) says what was done with "TrackAction.<VALUE>"
		for (TrackAction action : TrackAction.values())
			keys.add("TrackAction." + action.name());
		assertTranslated(keys, "Enum values");
	}
}
