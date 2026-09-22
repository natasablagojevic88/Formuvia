package rs.formuvia.utils;

public class ApiRoute {

	public static final String login = "/login";
	public static final String loginLogout = "/login/logout";

	public static final String session = "/session";
	public static final String sessionChangePassword = "/session/change-password";

	public static final String appuser = "/appuser";
	public static final String appuserId = "/appuser/{id}";
	public static final String appuserTable = "/appuser/table";
	public static final String appuserAllRoles = "/appuser/all-roles";

	public static final String role = "/role";
	public static final String roleId = "/role/{id}";
	public static final String roleTable = "/role/table";

	public static final String exportTable = "/export-table";

	public static final String history = "/history/{className}/{id}";

	public static final String model = "/model";
	public static final String modelTree = "/model/tree";
	public static final String modelId = "/model/{id}";

	public static final String modelColumn = "/model-column";
	public static final String modelColumnId = "/model-column/{id}";
	public static final String modelColumnTable = "/model-column/table";
}
