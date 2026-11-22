package rs.formuvia.model.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.exceptions.CommonException;

/**
 * A value of a record (UpdateObject.createValue) as it arrives from the front: always as text, and it must reach the
 * database as the type of the column, without being rounded or cut.
 */
class UpdateObjectTest {

	private static Object value(Object value, ColumnType columnType) {
		return UpdateObject.createValue(value, columnType, null, null, null, null, null);
	}

	private static void notWholeNumber(Object text, ColumnType columnType) {
		CommonException exception = assertThrows(CommonException.class, () -> value(text, columnType),
				columnType + " " + text);
		assertEquals("notWholeNumber", exception.getInMessage());
	}

	@Test
	void emptyValueIsNull() {
		assertNull(value(null, ColumnType.INTEGER));
		assertNull(value(" ", ColumnType.STRING));
	}

	@Test
	void wholeNumbersKeepTheirType() {
		assertEquals(7, value("7", ColumnType.INTEGER));
		assertEquals(-7, value(-7, ColumnType.INTEGER));
		assertEquals(3_000_000_000L, value("3000000000", ColumnType.LONG));
		// 7.0 is still the whole number seven
		assertEquals(7, value("7.0", ColumnType.INTEGER));
	}

	@Test
	void aDecimalInAWholeNumberColumnIsRefusedAndNotCut() {
		// "1.9" must not be saved as 1
		notWholeNumber("1.9", ColumnType.INTEGER);
		notWholeNumber("1.9", ColumnType.LONG);
	}

	@Test
	void aNumberTooBigForTheColumnIsRefusedAndNotTurnedNegative() {
		// 3000000000 as int would be -1294967296
		notWholeNumber("3000000000", ColumnType.INTEGER);
		notWholeNumber("10000000000000000000", ColumnType.LONG);
	}

	@Test
	void decimalsKeepEveryDigit() {
		assertEquals(new BigDecimal("12345678901234567.89"), value("12345678901234567.89", ColumnType.BIGDECIMAL));
	}

	@Test
	void otherTypesAreReadFromText() {
		UUID id = UUID.randomUUID();
		assertEquals(true, value("true", ColumnType.BOOLEAN));
		assertEquals(false, value(false, ColumnType.BOOLEAN));
		assertEquals("Ana", value("Ana", ColumnType.STRING));
		assertEquals(id, value(id.toString(), ColumnType.UUID));
		assertEquals(LocalDate.of(2026, 1, 31), value("2026-01-31", ColumnType.LOCALDATE));
		assertEquals(LocalDateTime.of(2026, 1, 31, 8, 30), value("2026-01-31T08:30", ColumnType.LOCALDATETIME));
		assertEquals(LocalTime.of(8, 30), value("08:30", ColumnType.LOCALTIME));
	}
}
