package rs.formuvia.model.utils;

import java.io.File;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.service.impl.SqlQueryWriterServiceImpl;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.model.entity.ModelJasper;

public class PrintModelJasper implements ExecuteQuery<Response> {
	private final HttpServletRequest httpServletRequest;
	private final UUID modelJasperId;
	private final UUID id;

	private CommonService commonService;
	private DatabaseService databaseService = new DatabaseServiceImpl();

	public static final String MIME_TYPE_EXPORT = "application/pdf";
	private final String MIME_TYPE_EXTENSION = ".pdf";

	public PrintModelJasper(HttpServletRequest httpServletRequest, UUID modelJasperId, UUID id) {
		this.httpServletRequest = httpServletRequest;
		this.modelJasperId = modelJasperId;
		this.id = id;
		this.commonService = new CommonServiceImpl(this.httpServletRequest);
	}

	@Override
	public Response execute(Connection connection) throws Exception {
		ModelJasper modelJasper = this.databaseService.findById(modelJasperId, ModelJasper.class, connection);
		this.commonService.checkRole(modelJasper.getModel().getPreviewRole().getCode());

		File file = DownloadModelFile.createFileFromPath(modelJasper.getFile().getPath(), modelJasper.getId());

		Map<String, Object> parameters = new HashMap<>();
		parameters.put(SqlQueryWriterServiceImpl.defaultIdColumn, this.id);
		parameters.put(JRParameter.REPORT_LOCALE, this.commonService.getUserLocale());
		JasperReport jasperReport = (JasperReport) JRLoader.loadObject(file);
		JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, connection);
		byte[] bytes = JasperExportManager.exportReportToPdf(jasperPrint);
		return ExportTableServiceImpl.createExcelResponse(bytes, modelJasper.getName() + MIME_TYPE_EXTENSION,
				MIME_TYPE_EXPORT);
	}

}
