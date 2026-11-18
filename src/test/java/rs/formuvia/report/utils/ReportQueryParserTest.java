package rs.formuvia.report.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;

/**
 * The SQL of a report names its parameters as ":code". ReportServiceImpl checks with ParameterTablesNamesFinder
 * that a parameter is really used in the query, and ReportPreviewServiceImpl turns ":code" into "?" with
 * QuestionMarkDeParser, keeping the order in which the values are bound. Both depend on JSqlParser and are the
 * first to break when its version is raised.
 */
class ReportQueryParserTest {

	private static Set<String> parameterNames(String sql) throws JSQLParserException {
		Statement statement = CCJSqlParserUtil.parse(sql);
		ParameterTablesNamesFinder finder = new ParameterTablesNamesFinder();
		finder.getTables(statement);
		return finder.getNames();
	}

	/** The same steps as ReportPreviewServiceImpl.getQueryPart. */
	private static Object[] questionMarks(String sql) throws JSQLParserException {
		Statement statement = CCJSqlParserUtil.parse(sql);
		StringBuilder builder = new StringBuilder();
		QuestionMarkDeParser expressionDeParser = new QuestionMarkDeParser();
		SelectDeParser selectDeParser = new SelectDeParser(expressionDeParser, builder);
		expressionDeParser.setSelectVisitor(selectDeParser);
		expressionDeParser.setBuilder(builder);
		statement.accept(new StatementDeParser(expressionDeParser, selectDeParser, builder), null);
		return new Object[] { builder.toString(), expressionDeParser.getOrder() };
	}

	@Test
	void findsParametersInWhere() throws JSQLParserException {
		assertEquals(Set.of("dateFrom", "dateTo", "status"), parameterNames(
				"select * from invoice where issued between :dateFrom and :dateTo and status = :status"));
	}

	@Test
	void findsParametersInSubqueriesJoinsAndFunctions() throws JSQLParserException {
		assertEquals(Set.of("customer", "year", "limit_amount"), parameterNames("""
				select i.number, c.name
				from invoice i
				join customer c on c.id = i.customer_id and c.code = :customer
				where extract(year from i.day) = :year
				  and i.id in (select invoice_id from item where amount > :limit_amount)"""));
	}

	@Test
	void findsParametersOfAProcedureCall() throws JSQLParserException {
		// an EXECUTE report runs a procedure; its parameters are in the argument list
		assertEquals(Set.of("from_day", "to_day"), parameterNames("call close_month(:from_day, :to_day)"));
	}

	@Test
	void aQueryWithoutParametersHasNone() throws JSQLParserException {
		assertEquals(Set.of(), parameterNames("select count(*) from invoice"));
	}

	@Test
	void namedParametersBecomeQuestionMarksInTheirOrder() throws JSQLParserException {
		Object[] part = questionMarks("select * from invoice where issued >= :from and issued <= :to and status = :status");

		assertEquals("SELECT * FROM invoice WHERE issued >= ? AND issued <= ? AND status = ?", part[0]);
		assertEquals(List.of("from", "to", "status"), part[1]);
	}

	@Test
	void aParameterUsedTwiceIsBoundTwice() throws JSQLParserException {
		Object[] part = questionMarks("select * from invoice where :day between valid_from and valid_to or issued = :day");

		assertEquals(List.of("day", "day"), part[1]);
	}

	@Test
	void textThatLooksLikeAParameterInsideQuotesStaysAsItIs() throws JSQLParserException {
		Object[] part = questionMarks("select ':not_a_parameter' as label from invoice where id = :id");

		assertEquals(List.of("id"), part[1]);
		assertEquals("SELECT ':not_a_parameter' AS label FROM invoice WHERE id = ?", part[0]);
	}

	@Test
	void invalidSqlIsRefused() {
		// ReportServiceImpl answers this with "invalidQuery"
		assertThrows(JSQLParserException.class, () -> parameterNames("select from where"));
	}
}
