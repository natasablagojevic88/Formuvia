package rs.formuvia.report.utils;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;

public class QuestionMarkDeParser extends ExpressionDeParser {

	@Getter
	private final List<String> order = new ArrayList<>();

	@Override
	public <S> StringBuilder visit(JdbcNamedParameter jdbcNamedParameter, S context) {
		this.order.add(jdbcNamedParameter.getName());
		return getBuilder().append("?");
	}
}
