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
	public static final String modelListOfValues = "/model/list-of-values/{id}";

	public static final String modelColumn = "/model-column";
	public static final String modelColumnId = "/model-column/{id}";
	public static final String modelColumnTable = "/model-column/list/{modelId}";
	public static final String modelColumnCondition = "/model-column/condition";
	public static final String modelColumnConditionId = "/model-column/condition/{id}";

	public static final String modelPreviewTable = "/preview/model/{modelId}";
	public static final String modelPreviewTableWithParent = "/preview/model/{modelId}/{parentId}";
	public static final String modelPreviewForm = "/preview/form/{modelId}";
	public static final String modelPreviewFormWithId = "/preview/form/{modelId}/{id}";
	public static final String modelPreviewFormWithParent = "/preview/form/{modelId}/parent/{parent}";
	public static final String modelPreviewUpdate = "/preview/update/{modelId}";
	public static final String modelPreviewDelete = "/preview/delete/{modelId}/{id}";
	public static final String modelPreviewHistory = "/preview/history/{modelId}/{id}";
	public static final String modelPreviewRow = "/preview/row/{modelId}/{id}";
	public static final String modelDownloadFile = "/preview/download/{modelId}/{id}/{columnName}";
	public static final String modelPreviewDownloadTemplate = "/preview/template/download/{modelId}";
	public static final String modelPreviewUploadTemplate = "/preview/template/upload/{modelId}/{fileTemplateId}";
	public static final String modelPreviewUploadTemplateWithParent = "/preview/template/upload/{modelId}/{fileTemplateId}/{parentId}";
	public static final String modelListFileVersion = "/preview/download/{modelId}/{id}/{columnName}/version";
	public static final String modelDownloadFileVersion = "/preview/download/{modelId}/{id}/{columnName}/version/{versionId}";

	public static final String fileUpload = "/file-upload";

}
