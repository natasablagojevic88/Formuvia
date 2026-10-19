package rs.formuvia.report.utils;

import java.util.HashSet;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.statement.execute.Execute;
import net.sf.jsqlparser.util.TablesNamesFinder;

@Setter
@Getter
public class ParameterTablesNamesFinder extends TablesNamesFinder<Void> {

	private final Set<String> names = new HashSet<>();

	@Override
	public <S> Void visit(JdbcNamedParameter jdbcNamedParameter, S context) {
		this.names.add(jdbcNamedParameter.getName());
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S> Void visit(Execute execute, S context) {
		if (execute.getExprList() != null)
			execute.getExprList().accept(this, context);
		return null;
	}
}
