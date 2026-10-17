package rs.formuvia.model.enums;

public enum ModelTriggerAction {

	INSERT("INSERT"), UPDATE("UPDATE"), INSERT_OR_UPDATE("INSERT OR UPDATE"), DELETE("DELETE"),
	ALL("INSERT OR UPDATE OR DELETE");

	public String action;

	private ModelTriggerAction(String action) {
		this.action = action;
	}

}
