package rs.formuvia.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class StringUtilsTest {

	@Test
	void hasText() {
		assertFalse(StringUtils.hasText(null));
		assertFalse(StringUtils.hasText(""));
		assertFalse(StringUtils.hasText("   "));
		assertFalse(StringUtils.hasText("\t\n"));
		assertTrue(StringUtils.hasText("a"));
		assertTrue(StringUtils.hasText("  a  "));
	}

	@Test
	void notNullAndIsNullAreOpposite() {
		Object[] values = { null, "", "  ", "x", 0, false, UUID.randomUUID() };
		for (Object value : values)
			assertEquals(!StringUtils.notNull(value), StringUtils.isNull(value), String.valueOf(value));
	}

	@Test
	void blankTextCountsAsNull() {
		assertTrue(StringUtils.isNull(null));
		assertTrue(StringUtils.isNull(""));
		assertTrue(StringUtils.isNull(" "));
		assertFalse(StringUtils.isNull("x"));
	}

	@Test
	void zeroAndFalseAreValues() {
		// a filter "amount = 0" or "active = false" must not be thrown away as empty
		assertTrue(StringUtils.notNull(0));
		assertTrue(StringUtils.notNull(false));
	}

	@Test
	void createStringArray() {
		assertEquals("", StringUtils.createStringArray(new String[0]));
		assertEquals("id", StringUtils.createStringArray(new String[] { "id" }));
		assertEquals("id,name,active", StringUtils.createStringArray(new String[] { "id", "name", "active" }));
	}

	@Test
	void createStringArrayWithDelimiter() {
		assertEquals("", StringUtils.createStringArrayWithDelimiter(new String[0], ", "));
		assertEquals("id", StringUtils.createStringArrayWithDelimiter(new String[] { "id" }, ", "));
		assertEquals("id, name", StringUtils.createStringArrayWithDelimiter(new String[] { "id", "name" }, ", "));
	}
}
