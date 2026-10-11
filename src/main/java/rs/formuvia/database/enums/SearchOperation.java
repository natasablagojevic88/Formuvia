package rs.formuvia.database.enums;

public enum SearchOperation {

	EQUALS("="), NOT_EQUALS("!="), STARTS_WITH("like"), ENDS_WITH("like"), CONTAINS("like"), LESS_THEN("<"),
	GREATER_THEN(">"), LESS_THEN_OR_EQUALS("<="), GREATER_THEN_OR_EQUALS(">="), BETWEEN("between"), IS_NULL("is null"),
	IS_NOT_NULL("is not null");

	public String databaseMark;

	private SearchOperation(String databaseMark) {
		this.databaseMark = databaseMark;
	}

}
