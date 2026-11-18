package rs.formuvia.database.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** numberOfPages of every table: the paginator of the front shows it as it comes. */
class CreateDatabaseTableTest {

	@Test
	void noRowsNoPages() {
		assertEquals(0, CreateDatabaseTable.numberOfPages(0L, 10));
	}

	@Test
	void aPageThatIsNotFullIsStillAPage() {
		assertEquals(1, CreateDatabaseTable.numberOfPages(1L, 10));
		assertEquals(1, CreateDatabaseTable.numberOfPages(10L, 10));
		assertEquals(2, CreateDatabaseTable.numberOfPages(11L, 10));
		assertEquals(3, CreateDatabaseTable.numberOfPages(25L, 10));
	}

	@Test
	void everythingOnOnePageWhenThePageSizeIsNotGiven() {
		// DatabaseParameter.pageSize is Integer.MAX_VALUE by default
		assertEquals(1, CreateDatabaseTable.numberOfPages(5000L, new DatabaseParameter().getPageSize()));
	}
}
