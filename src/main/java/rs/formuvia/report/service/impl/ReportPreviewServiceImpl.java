package rs.formuvia.report.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.ResourceBundleService;
import rs.formuvia.common.service.impl.ExportTableServiceImpl;
import rs.formuvia.common.service.impl.ResourceBundleServiceImpl;
import rs.formuvia.common.utils.ExcelUtils;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.model.utils.UpdateObject;
import rs.formuvia.report.dto.ReportDTO;
import rs.formuvia.report.dto.ReportGroupDTO;
import rs.formuvia.report.dto.ReportInfoDTO;
import rs.formuvia.report.dto.ReportParameterDTO;
import rs.formuvia.report.dto.ReportResultColumnDTO;
import rs.formuvia.report.dto.ReportResultDTO;
import rs.formuvia.report.enums.ReportType;
import rs.formuvia.report.service.ReportPreviewService;
import rs.formuvia.report.utils.CreateReportInfo;
import rs.formuvia.report.utils.ExecuteReportPreview;
import rs.formuvia.report.utils.QuestionMarkDeParser;
import rs.formuvia.report.utils.ReportPreviewJasper;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

@Service
public class ReportPreviewServiceImpl implements ReportPreviewService {

	@Inject
	private CommonService commonService;

	@Inject
	private DatabaseService databaseService;

	@Context
	private HttpServletRequest httpServletRequest;

	@Inject
	private ResourceBundleService resourceBundleService;

	private final String SHEET_NAME = "report";
	private final String REPORT_FILE_NAME = "report.xlsx";
	private final Integer NUMBER_OF_DECIMAL = 2;

	private void checkRole(UUID reportId) {
		ReportDTO reportDTO = findReport(reportId);
		ReportGroupDTO reportGroupDTO = StaticData.reportGroups.stream()
				.filter(a -> a.getId().equals(reportDTO.getReportGroupId())).findFirst().get();
		commonService.checkRole(reportGroupDTO.getRoleCode());
	}

	public static ReportDTO findReport(UUID reportId) {
		return StaticData.reports.stream().filter(a -> a.getId().equals(reportId)).findFirst().get();
	}

	@SuppressWarnings("unchecked")
	@Override
	public ReportResultDTO getReportPreviewTable(LinkedHashMap<String, Object> parameters, UUID reportId) {
		databaseService = databaseService == null ? new DatabaseServiceImpl() : databaseService;
		checkRole(reportId);
		ReportDTO reportDTO = findReport(reportId);
		if (!reportDTO.getType().equals(ReportType.SQL))
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "wrongReportType", reportId);
		Object[] sqlPart = getQueryPart(reportDTO.getSqlQuery());
		String query = sqlPart[0].toString();
		List<String> orderParameters = (List<String>) sqlPart[1];
		Map<Integer, Object> parameterQuery = createParameters(orderParameters, parameters, reportId);
		ExecuteReportPreview executeReportPreview = new ExecuteReportPreview(httpServletRequest, query, parameterQuery);

		return this.databaseService.executeQuery(executeReportPreview);
	}

	private Map<Integer, Object> createParameters(List<String> orderParameters,
			LinkedHashMap<String, Object> parameters, UUID reportId) {
		Map<Integer, Object> parameterQuery = new HashMap<>();

		int count = 0;
		for (String parameter : orderParameters) {
			count++;

			ReportParameterDTO parameterDTO = CreateReportInfo.getParametersFromList(reportId, parameter);
			Object value = UpdateObject.createValue(parameters.get(parameter), parameterDTO.getColumnType(), null, null,
					null, parameters, commonService);

			parameterQuery.put(count, value);
		}
		return parameterQuery;
	}

	private Object[] getQueryPart(String query) {
		Statement statement;
		try {
			statement = CCJSqlParserUtil.parse(query);
		} catch (JSQLParserException e) {
			throw new WebApplicationException(e);
		}

		StringBuilder builder = new StringBuilder();
		QuestionMarkDeParser expressionDeParser = new QuestionMarkDeParser();
		SelectDeParser selectDeParser = new SelectDeParser(expressionDeParser, builder);
		expressionDeParser.setSelectVisitor(selectDeParser);
		expressionDeParser.setBuilder(builder);

		statement.accept(new StatementDeParser(expressionDeParser, selectDeParser, builder), null);

		return new Object[] { builder.toString(), expressionDeParser.getOrder() };
	}

	@Override
	public Response getReportPreviewExcel(LinkedHashMap<String, Object> parameters, UUID reportId) {
		resourceBundleService = resourceBundleService == null ? new ResourceBundleServiceImpl() : resourceBundleService;
		ReportResultDTO reportResultDTO = getReportPreviewTable(parameters, reportId);
		XSSFWorkbook wb = new XSSFWorkbook();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		XSSFSheet sheet = wb.createSheet(SHEET_NAME);

		XSSFRow row = sheet.createRow(0);
		int index = -1;
		for (ReportResultColumnDTO column : reportResultDTO.getColumns()) {
			index++;
			ExcelUtils.createTableTitleCell(wb, row, index, column.getName());
		}

		int indexRow = 0;
		for (LinkedHashMap<String, Object> item : reportResultDTO.getList()) {
			indexRow++;
			row = sheet.createRow(indexRow);
			int indexColumn = -1;
			for (ReportResultColumnDTO column : reportResultDTO.getColumns()) {
				indexColumn++;
				if (StringUtils.isNull(item.get(column.getCode())))
					continue;

				ExcelUtils.createCell(wb, row, indexColumn, column.getColumnType(), this.resourceBundleService,
						item.get(column.getCode()), null, NUMBER_OF_DECIMAL);
			}
		}

		for (int i = 0; i < reportResultDTO.getColumns().size(); i++)
			sheet.autoSizeColumn(i);

		try {

			wb.write(baos);

			wb.close();
		} catch (IOException e) {
			throw new WebApplicationException(e);
		}

		return ExportTableServiceImpl.createExcelResponse(baos.toByteArray(), REPORT_FILE_NAME,
				ExportTableServiceImpl.EXCEL_CONTENT_TYPE);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void getReportExecute(LinkedHashMap<String, Object> parameters, UUID reportId) {
		databaseService = databaseService == null ? new DatabaseServiceImpl() : databaseService;
		checkRole(reportId);
		ReportDTO reportDTO = findReport(reportId);
		if (!reportDTO.getType().equals(ReportType.EXECUTE))
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "wrongReportType", reportId);
		Object[] sqlPart = getQueryPart(reportDTO.getSqlQuery());
		String query = sqlPart[0].toString();
		List<String> orderParameters = (List<String>) sqlPart[1];
		Map<Integer, Object> parameterQuery = createParameters(orderParameters, parameters, reportId);
		this.databaseService.executeUpdateQuery(query, parameterQuery);

	}

	@Override
	public ReportInfoDTO getReportInfo(UUID reportId) {
		databaseService = databaseService == null ? new DatabaseServiceImpl() : databaseService;
		checkRole(reportId);
		CreateReportInfo createReportInfo = new CreateReportInfo(httpServletRequest, reportId);

		return databaseService.executeQuery(createReportInfo);
	}

	@Override
	public Response getReportPreviewJasper(LinkedHashMap<String, Object> parameters, UUID reportId) {
		checkRole(reportId);
		ReportPreviewJasper reportPreviewJasper = new ReportPreviewJasper(httpServletRequest, parameters, reportId);
		return databaseService.executeQuery(reportPreviewJasper);
	}

}
