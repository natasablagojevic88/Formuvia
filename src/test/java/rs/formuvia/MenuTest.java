package rs.formuvia;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import rs.formuvia.utils.RoleList;

/**
 * resources/menu.xml is read once, when the application starts (AppStartUpImpl.loadMenu); a mistake in it is
 * seen only after a deploy, as a missing or untranslated item.
 */
class MenuTest {

	static final Path MENU = ResourceBundleTest.RESOURCES.resolve("menu.xml");
	static final Path SCHEMA = ResourceBundleTest.RESOURCES.resolve("menu.xsd");

	/** Every item of the menu, at any depth. */
	static List<Element> items = new ArrayList<>();

	@BeforeAll
	static void read() throws Exception {
		items.clear();
		Document document = new SAXBuilder().build(MENU.toFile());
		collect(document.getRootElement());
	}

	private static void collect(Element parent) {
		for (Element item : parent.getChildren("item")) {
			items.add(item);
			collect(item);
		}
	}

	static List<String> urls() {
		return items.stream().map(item -> item.getAttributeValue("url")).filter(url -> url != null).toList();
	}

	@Test
	void menuFollowsItsSchema() {
		assertDoesNotThrow(() -> SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
				.newSchema(SCHEMA.toFile()).newValidator().validate(new StreamSource(MENU.toFile())));
	}

	@Test
	void everyNameIsTranslatedInTheMenuBundle() throws IOException {
		for (String locale : ResourceBundleTest.LOCALES) {
			Properties menu = ResourceBundleTest.load("menu", locale);
			List<String> missing = items.stream().map(item -> item.getAttributeValue("name"))
					.filter(name -> !menu.containsKey(name)).toList();
			assertTrue(missing.isEmpty(), "resource_bundle_menu_" + locale + " is missing: " + missing);
		}
	}

	@Test
	void anItemEitherOpensAPageOrHoldsOtherItems() {
		for (Element item : items) {
			boolean hasChildren = !item.getChildren("item").isEmpty();
			boolean hasUrl = item.getAttributeValue("url") != null;
			assertTrue(hasChildren != hasUrl, item.getAttributeValue("name")
					+ (hasChildren ? " holds other items and also has a url" : " has neither a url nor items"));
		}
	}

	@Test
	void noPageIsInTheMenuTwice() {
		Set<String> seen = new HashSet<>();
		for (String url : urls())
			assertTrue(seen.add(url), url + " is in the menu twice");
	}

	@Test
	void everyItemHasAnIcon() {
		for (Element item : items) {
			String icon = item.getAttributeValue("icon");
			assertFalse(icon == null || icon.isBlank(), item.getAttributeValue("name") + " has no icon");
			assertTrue(icon.startsWith("fa-"), item.getAttributeValue("name") + ": the icon is a Font Awesome class");
		}
	}

	@Test
	void rolesAreTheOnesTheApplicationCreates() throws IllegalAccessException {
		// the roles of RoleList are made when the application starts; another role may not exist yet
		Set<String> roles = new HashSet<>();
		for (Field field : RoleList.class.getFields())
			if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class)
				roles.add((String) field.get(null));
		for (Element item : items) {
			String role = item.getAttributeValue("role");
			assertTrue(role == null || roles.contains(role),
					item.getAttributeValue("name") + " needs the role \"" + role + "\", which is not in RoleList");
		}
	}
}
