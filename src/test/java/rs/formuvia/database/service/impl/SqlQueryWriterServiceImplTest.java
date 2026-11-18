package rs.formuvia.database.service.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.database.enums.Direction;
import rs.formuvia.database.enums.SearchOperation;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.QueryColumnInfo;
import rs.formuvia.database.utils.QueryDatabaseOrder;
import rs.formuvia.database.utils.QueryTableInfo;

/**
 * Every table of the application is read through these queries, so the exact text matters: the alias of the
 * main table, the order of the joins and of the "?" marks the parameters are bound to.
 */
class SqlQueryWriterServiceImplTest {

	private final SqlQueryWriterServiceImpl writer = new SqlQueryWriterServiceImpl();

	/** app_user with its own columns only. */
	private static QueryTableInfo userTable() {
		QueryTableInfo table = new QueryTableInfo();
		table.setName("app_user");
		table.getColumns().add(QueryColumnInfo.valueOf("id", "id", ColumnType.UUID));
		table.getColumns().add(QueryColumnInfo.valueOf("username", "username", ColumnType.STRING));
		table.getColumns().add(QueryColumnInfo.valueOf("active", "active", ColumnType.BOOLEAN));
		return table;
	}

	private static QueryColumnInfo joined(String fieldName, String columnName, List<String> paths,
			List<String> tables) {
		QueryColumnInfo column = QueryColumnInfo.valueOf(fieldName, columnName, ColumnType.STRING);
		column.getPaths().addAll(paths);
		column.getTableName().addAll(tables);
		return column;
	}

	/** The queries are written with the line separator of the system; the expected text uses "\n". */
	private static String n(String query) {
		return query.replace(System.lineSeparator(), "\n");
	}

	@Test
	void selectWithoutFiltersHasColumnsFromAndPaging() {
		DatabaseParameter parameter = new DatabaseParameter();
		parameter.setPageIndex(2);
		parameter.setPageSize(25);

		String query = n(writer.createSelectQuery(userTable(), parameter));

		assertEquals("""
				SELECT
				a.id,
				a.username,
				a.active
				FROM
				app_user a
				LIMIT 25 OFFSET 50""", query);
	}

	@Test
	void selectJoinsEveryTableOnTheWayOnceAndInOrder() {
		// role_app_user -> app_user -> role ; app_user is reached by two columns but joined once
		QueryTableInfo table = new QueryTableInfo();
		table.setName("role_app_user");
		table.getColumns().add(QueryColumnInfo.valueOf("id", "id", ColumnType.UUID));
		table.getColumns().add(joined("appUserUsername", "username", List.of("app_user_id"), List.of("app_user")));
		table.getColumns().add(joined("appUserName", "name", List.of("app_user_id"), List.of("app_user")));
		table.getColumns().add(joined("appUserRoleName", "name", List.of("app_user_id", "main_role_id"),
				List.of("app_user", "role")));

		String query = n(writer.createSelectQuery(table, new DatabaseParameter()));

		assertEquals("""
				SELECT
				a.id,
				t1.username,
				t1.name,
				t2.name
				FROM
				role_app_user a
				LEFT JOIN app_user t1
				ON a.app_user_id=t1.id
				LEFT JOIN role t2
				ON t1.main_role_id=t2.id
				LIMIT 2147483647 OFFSET 0""", query);
	}

	@Test
	void selectWhereAndOrderUseTheAliasOfTheJoinedTable() {
		QueryTableInfo table = userTable();
		table.getColumns().add(joined("roleName", "name", List.of("role_id"), List.of("role")));
		DatabaseParameter parameter = DatabaseParameter.valueOf(
				DatabaseFilter.valueOf("username", SearchOperation.CONTAINS, "ana"),
				DatabaseFilter.valueOf("roleName", "admin"));
		parameter.getOrders().add(QueryDatabaseOrder.valueOf("roleName", Direction.DESC));
		parameter.getOrders().add(QueryDatabaseOrder.valueOf("username", Direction.ASC));
		parameter.setPageSize(10);

		String query = n(writer.createSelectQuery(table, parameter));

		assertEquals("""
				SELECT
				a.id,
				a.username,
				a.active,
				t1.name
				FROM
				app_user a
				LEFT JOIN role t1
				ON a.role_id=t1.id
				WHERE
				lower(a.username) like lower('%'||?||'%')
				AND t1.name = ?
				ORDER BY
				t1.name DESC,
				a.username ASC
				LIMIT 10 OFFSET 0""", query);
	}

	@Test
	void emptyFiltersAreLeftOutOfWhere() {
		DatabaseParameter parameter = DatabaseParameter.valueOf(
				DatabaseFilter.valueOf("username", SearchOperation.CONTAINS, "  "),
				DatabaseFilter.valueOf("active", SearchOperation.EQUALS, null));

		String query = n(writer.createSelectQuery(userTable(), parameter));

		assertTrue(!query.contains("WHERE"), query);
	}

	@Test
	void totalCountsWithTheSameFilterAndWithoutPaging() {
		DatabaseParameter parameter = DatabaseParameter.valueOf(DatabaseFilter.valueOf("username", "ana"));
		parameter.getOrders().add(QueryDatabaseOrder.valueOf("username", Direction.ASC));

		String query = n(writer.createTotalQuery(userTable(), parameter, null));

		assertEquals("""
				select count(*)
				FROM
				app_user a
				WHERE
				a.username = ?""", query);
	}

	@Test
	void totalAddsSumsOfTheGivenColumns() {
		String query = n(writer.createTotalQuery(userTable(), new DatabaseParameter(),
				new String[] { "a.amount", "a.tax" }));

		assertEquals("""
				select count(*), coalesce(sum(a.amount),0), coalesce(sum(a.tax),0)
				FROM
				app_user a""", query);
	}

	@Test
	void conditionForEveryOperation() {
		assertEquals("a.x = ?", SqlQueryWriterServiceImpl.conditionString("a.x", SearchOperation.EQUALS));
		assertEquals("a.x != ?", SqlQueryWriterServiceImpl.conditionString("a.x", SearchOperation.NOT_EQUALS));
		assertEquals("a.x < ?", SqlQueryWriterServiceImpl.conditionString("a.x", SearchOperation.LESS_THEN));
		assertEquals("a.x > ?", SqlQueryWriterServiceImpl.conditionString("a.x", SearchOperation.GREATER_THEN));
		assertEquals("a.x <= ?",
				SqlQueryWriterServiceImpl.conditionString("a.x", SearchOperation.LESS_THEN_OR_EQUALS));
		assertEquals("a.x >= ?",
				SqlQueryWriterServiceImpl.conditionString("a.x", SearchOperation.GREATER_THEN_OR_EQUALS));
		assertEquals("a.x between ? and ?", SqlQueryWriterServiceImpl.conditionString("a.x", SearchOperation.BETWEEN));
		assertEquals("a.x is null", SqlQueryWriterServiceImpl.conditionString("a.x", SearchOperation.IS_NULL));
		assertEquals("a.x is not null",
				SqlQueryWriterServiceImpl.conditionString("a.x", SearchOperation.IS_NOT_NULL));
		assertEquals("lower(a.x) like lower('%'||?||'%')",
				SqlQueryWriterServiceImpl.conditionString("a.x", SearchOperation.CONTAINS));
		assertEquals("lower(a.x) like lower(?||'%')",
				SqlQueryWriterServiceImpl.conditionString("a.x", SearchOperation.STARTS_WITH));
		assertEquals("lower(a.x) like lower('%'||?)",
				SqlQueryWriterServiceImpl.conditionString("a.x", SearchOperation.ENDS_WITH));
	}

	@ParameterizedTest
	@EnumSource(SearchOperation.class)
	void everyOperationHasAsManyMarksAsParameters(SearchOperation operation) {
		// the "?" in the condition and the values bound to it must always match
		String condition = SqlQueryWriterServiceImpl.conditionString("a.x", operation);
		DatabaseFilter filter = DatabaseFilter.valueOf("x", operation, "1", "2");
		filter.setColumnType(ColumnType.INTEGER);

		long marks = condition.chars().filter(c -> c == '?').count();

		assertEquals(marks, SqlQueryWriterServiceImpl.createParametersForFilters(filter).length, condition);
	}

	@Test
	void clearKeepsNullChecksAndCompleteFiltersOnly() {
		List<DatabaseFilter> kept = SqlQueryWriterServiceImpl.clearDatabaseFilters(List.of(
				DatabaseFilter.valueOf("a", SearchOperation.IS_NULL, null),
				DatabaseFilter.valueOf("b", SearchOperation.IS_NOT_NULL, null),
				DatabaseFilter.valueOf("c", SearchOperation.BETWEEN, "1", null),
				DatabaseFilter.valueOf("d", SearchOperation.BETWEEN, "1", "5"),
				DatabaseFilter.valueOf("e", SearchOperation.EQUALS, ""),
				DatabaseFilter.valueOf("f", SearchOperation.EQUALS, " "),
				DatabaseFilter.valueOf("g", SearchOperation.EQUALS, "x")));

		assertEquals(List.of("a", "b", "d", "g"), kept.stream().map(DatabaseFilter::getField).toList());
	}

	@Test
	void parametersAreNumberedFromOneAndSkipEmptyFilters() {
		DatabaseFilter between = DatabaseFilter.valueOf("amount", SearchOperation.BETWEEN, "1.5", "10");
		between.setColumnType(ColumnType.BIGDECIMAL);
		DatabaseFilter empty = DatabaseFilter.valueOf("name", "", ColumnType.STRING);
		DatabaseFilter isNull = DatabaseFilter.valueOf("note", SearchOperation.IS_NULL, null);
		isNull.setColumnType(ColumnType.STRING);
		DatabaseFilter day = DatabaseFilter.valueOf("day", "2026-11-18", ColumnType.LOCALDATE);

		Map<Integer, Object> parameters = writer.createParameters(List.of(between, empty, isNull, day));

		assertEquals(Map.of(1, new BigDecimal("1.5"), 2, new BigDecimal("10"), 3, LocalDate.of(2026, 11, 18)),
				parameters);
	}

	@Test
	void parametersHaveTheTypeOfTheColumn() {
		UUID id = UUID.randomUUID();
		assertArrayEquals(new Object[] { 7 }, values("7", ColumnType.INTEGER));
		assertArrayEquals(new Object[] { 7L }, values("7", ColumnType.LONG));
		assertArrayEquals(new Object[] { new BigDecimal("7.25") }, values("7.25", ColumnType.BIGDECIMAL));
		assertArrayEquals(new Object[] { true }, values("true", ColumnType.BOOLEAN));
		assertArrayEquals(new Object[] { "Ana" }, values("Ana", ColumnType.STRING));
		assertArrayEquals(new Object[] { id }, values(id.toString(), ColumnType.UUID));
		assertArrayEquals(new Object[] { LocalDate.of(2026, 1, 31) }, values("2026-01-31", ColumnType.LOCALDATE));
		assertArrayEquals(new Object[] { LocalDateTime.of(2026, 1, 31, 8, 30) },
				values("2026-01-31T08:30", ColumnType.LOCALDATETIME));
		assertArrayEquals(new Object[] { LocalTime.of(8, 30) }, values("08:30", ColumnType.LOCALTIME));
		assertArrayEquals(new Object[0], values("x", ColumnType.FILE));
	}

	@Test
	void textThatIsNotANumberIsRefused() {
		assertThrows(NumberFormatException.class, () -> values("abc", ColumnType.INTEGER));
	}

	private static Object[] values(String value, ColumnType columnType) {
		return SqlQueryWriterServiceImpl.createParametersForFilters(DatabaseFilter.valueOf("x", value, columnType));
	}

	@Test
	void insertLeavesTheIdToTheDatabaseAndReturnsEveryColumn() {
		String query = n(writer.insertQuery(new String[] { "id", "username", "active" }, "app_user"));

		assertEquals("""
				INSERT INTO
				app_user
				(
				username,
				active
				)
				values
				(
				?,
				?
				)
				RETURNING
				id,username,active""", query);
	}

	@Test
	void updateOfSeveralColumnsUsesARowValue() {
		String query = n(writer.updateQuery(new String[] { "id", "username", "active" }, "app_user"));

		assertEquals("""
				UPDATE\s
				app_user
				SET
				(
				username,
				active
				)
				=
				(
				?,
				?
				)
				WHERE id = ?
				RETURNING
				id,username,active""", query);
	}

	@Test
	void updateOfOneColumnHasNoParentheses() {
		// "SET (x) = (?)" is refused by PostgreSQL, so a single column must be written without them
		String query = n(writer.updateQuery(new String[] { "id", "username" }, "app_user"));

		assertEquals("""
				UPDATE\s
				app_user
				SET
				username
				=
				?
				WHERE id = ?
				RETURNING
				id,username""", query);
	}

	@Test
	void deleteById() {
		assertEquals("DELETE FROM app_user\nWHERE id=?", n(writer.deleteQuery("app_user")));
	}
}
