package rs.formuvia.report.utils;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.common.service.impl.ExportTableServiceImpl;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.model.utils.DownloadModelFile;
import rs.formuvia.model.utils.PrintModelJasper;
import rs.formuvia.model.utils.UpdateObject;
import rs.formuvia.report.dto.ReportDTO;
import rs.formuvia.report.dto.ReportParameterDTO;
import rs.formuvia.report.enums.ReportType;
import rs.formuvia.report.service.impl.ReportPreviewServiceImpl;

public class ReportPreviewJasper implements ExecuteQuery<Response> {
	private final HttpServletRequest httpServletRequest;
	private final LinkedHashMap<String, Object> parameters;
	private final UUID reportId;

	private CommonService commonService;
	private final String FILE_NAME = "report.pdf";

	public ReportPreviewJasper(HttpServletRequest httpServletRequest, LinkedHashMap<String, Object> parameters,
			UUID reportId) {
		this.httpServletRequest = httpServletRequest;
		this.parameters = parameters;
		this.reportId = reportId;
		this.commonService = new CommonServiceImpl(this.httpServletRequest);
	}

	@Override
	public Response execute(Connection connection) throws Exception {
		ReportDTO reportDTO = ReportPreviewServiceImpl.findReport(reportId);
		if (!reportDTO.getType().equals(ReportType.JASPER))
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "frontReportFormat", reportDTO.getId());

		File jasperFile = DownloadModelFile.createFileFromPath(reportDTO.getJasperFilePath(), reportDTO.getId());
		JasperReport jasperReport = (JasperReport) JRLoader.loadObject(jasperFile);
		List<ReportParameterDTO> list = CreateReportInfo.getParametersForReportId(this.reportId);
		Map<String, Class<?>> jasperTypes = Arrays.stream(jasperReport.getParameters())
				.filter(p -> !p.isSystemDefined())
				.collect(Collectors.toMap(JRParameter::getName, JRParameter::getValueClass));
		for (ReportParameterDTO parameterDTO : list) {

			Object value = UpdateObject.createValue(parameters.get(parameterDTO.getCode()),
					parameterDTO.getColumnType(), null, null, null, parameters, commonService);

			if (value instanceof LocalDate date) {
				value = java.sql.Date.valueOf(date);
			}
			;

			if (value instanceof LocalDateTime date) {
				value = java.sql.Timestamp.valueOf(date);
			}
			;

			if (value instanceof LocalTime date) {
				value = java.sql.Time.valueOf(date);
			}
			;

			Class<?> target = jasperTypes.get(parameterDTO.getCode());
			if (value instanceof Number number && target != null && !target.isInstance(value))
				value = toJasperNumber(number, target, parameterDTO.getCode());

			parameters.put(parameterDTO.getCode(), value);
		}
		parameters.put(JRParameter.REPORT_LOCALE, this.commonService.getUserLocale());

		JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, connection);
		byte[] bytes = JasperExportManager.exportReportToPdf(jasperPrint);

		return ExportTableServiceImpl.createExcelResponse(bytes, FILE_NAME, PrintModelJasper.MIME_TYPE_EXPORT);
	}

	private Object toJasperNumber(Number number, Class<?> target, String code) {
		BigDecimal bigDecimal = new BigDecimal(number.toString());
		try {
			if (target == Integer.class)
				return bigDecimal.intValueExact();
			if (target == Long.class)
				return bigDecimal.longValueExact();
			if (target == Short.class)
				return bigDecimal.shortValueExact();
			if (target == Double.class)
				return bigDecimal.doubleValue();
			if (target == Float.class)
				return bigDecimal.floatValue();
			if (target == BigDecimal.class)
				return bigDecimal;
			if (target == BigInteger.class)
				return bigDecimal.toBigIntegerExact();
		} catch (ArithmeticException e) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "notWholeNumber", code);
		}
		return number; // Object, Number... – Jasper takes it as it is
	}

}
