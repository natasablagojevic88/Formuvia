package rs.formuvia.report.utils;

import java.io.File;
import java.net.HttpURLConnection;
import java.sql.Connection;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;

import jakarta.servlet.http.HttpServletRequest;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.execute.Execute;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import rs.formuvia.common.entity.FileUpload;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.database.enums.SearchOperation;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.exceptions.NotNullException;
import rs.formuvia.exceptions.UniqueException;
import rs.formuvia.model.entity.ModelFile;
import rs.formuvia.model.utils.UpdateModelJasper;
import rs.formuvia.model.utils.UpdateObject;
import rs.formuvia.report.dto.ReportDTO;
import rs.formuvia.report.entity.Report;
import rs.formuvia.report.enums.ReportType;
import rs.formuvia.utils.StringUtils;

public class UpdateReport implements ExecuteQuery<ReportDTO> {
	private final HttpServletRequest httpServletRequest;
	private final ReportDTO reportDTO;

	private DatabaseService databaseService = new DatabaseServiceImpl();
	private CommonService commonService;
	private ModelMapper modelMapper = new ModelMapper();

	public UpdateReport(HttpServletRequest httpServletRequest, ReportDTO reportDTO) {
		this.httpServletRequest = httpServletRequest;
		this.reportDTO = reportDTO;
		this.commonService = new CommonServiceImpl(this.httpServletRequest);
	}

	@Override
	public ReportDTO execute(Connection connection) throws Exception {
		Report report = StringUtils.isNull(reportDTO.getId()) ? new Report()
				: this.databaseService.findById(reportDTO.getId(), Report.class, connection);

		if (reportDTO.getType().equals(ReportType.EXECUTE) || reportDTO.getType().equals(ReportType.SQL)) {
			if (StringUtils.isNull(reportDTO.getSqlQuery()))
				throw new NotNullException(UniqueException.findFieldFromList(ReportDTO.class, "sqlQuery"));
			report.setJasterFile(null);
		} else {
			reportDTO.setSqlQuery(null);

			if (StringUtils.isNull(reportDTO.getId())) {
				if (StringUtils.isNull(reportDTO.getFileUpload()))
					throw new NotNullException(UniqueException.findFieldFromList(ReportDTO.class, "fileUpload"));
			} else {
				if (StringUtils.isNull(reportDTO.getFileUpload()) && StringUtils.isNull(report.getJasterFile()))
					throw new NotNullException(UniqueException.findFieldFromList(ReportDTO.class, "fileUpload"));
			}

			if (StringUtils.notNull(reportDTO.getFileUpload()))
				if (StringUtils.notNull(reportDTO.getFileUpload().getId())) {
					FileUpload fileUpload = this.databaseService.findById(reportDTO.getFileUpload().getId(),
							FileUpload.class, connection);
					File file = UpdateObject.createUploadFile(fileUpload, commonService);
					try {
						@SuppressWarnings("unused")
						JasperReport jasperReport = ((JasperReport) JRLoader.loadObject(file));
					} catch (JRException | ClassCastException e) {
						throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "invalidJasperFile",
								file.getName());
					}

					ModelFile modelFile = UpdateModelJasper.createModelFile(
							report.getJasterFile() == null ? null : report.getJasterFile().getId(), file,
							reportDTO.getFileUpload().getFileName(), fileUpload.getPath(), connection);
					report.setJasterFile(modelFile);
				}
		}

		DatabaseParameter uniqueParameter = DatabaseParameter.singleFilter(
				new DatabaseFilter[] { DatabaseFilter.valueOf("reportGroupId", reportDTO.getReportGroupId().toString()),
						DatabaseFilter.valueOf("name", reportDTO.getName()) });

		if (StringUtils.notNull(reportDTO.getId())) {
			uniqueParameter.getFilters()
					.add(DatabaseFilter.valueOf("id", SearchOperation.NOT_EQUALS, reportDTO.getId().toString()));
			if (!reportDTO.getType().equals(report.getType())) {
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "reportTypeNotChangeable",
						report.getName());
			}
		}

		if (this.databaseService.exists(uniqueParameter, ReportDTO.class, connection))
			throw new UniqueException(UniqueException.findFieldFromList(ReportDTO.class, "name"), reportDTO.getName());

		checkSqlQuery(reportDTO.getType(), reportDTO.getSqlQuery());

		customMapper().map(reportDTO, report);

		report = this.databaseService.save(report, connection);

		return this.modelMapper.map(report, ReportDTO.class);
	}

	private ModelMapper customMapper() {
		ModelMapper modelMapper = new ModelMapper();
		modelMapper.addMappings(new PropertyMap<ReportDTO, Report>() {

			@Override
			protected void configure() {
				skip(destination.getJasterFile());

			}
		});
		return modelMapper;
	}

	private void checkSqlQuery(ReportType reportType, String sqlQuery) {

		if (!StringUtils.hasText(sqlQuery)) {
			return;
		}

		Statements statements;
		try {
			statements = CCJSqlParserUtil.parseStatements(sqlQuery);
		} catch (JSQLParserException e) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "invalidQuery", sqlQuery);
		}

		if (statements.size() != 1) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "onlyOneQueryAllowed", sqlQuery);
		}

		Statement statement = statements.getFirst();

		if (reportType.equals(ReportType.SQL)) {
			if (!(statement instanceof Select)) {
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "onlySelectAllowed", sqlQuery);
			}
		} else if (reportType.equals(ReportType.EXECUTE)) {
			if (!(statement instanceof Execute || statement instanceof Update || statement instanceof Insert
					|| statement instanceof Delete)) {
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "onlyExecuteAllowed", sqlQuery);
			}
		}

	}

}
