package rs.formuvia.report.service.impl;

import java.io.File;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.jvnet.hk2.annotations.Service;
import org.modelmapper.ModelMapper;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.database.enums.Direction;
import rs.formuvia.database.enums.SearchOperation;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.database.utils.QueryDatabaseOrder;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.exceptions.UniqueException;
import rs.formuvia.model.utils.DownloadModelFile;
import rs.formuvia.model.utils.UpdateColumnModel;
import rs.formuvia.report.dto.ReportDTO;
import rs.formuvia.report.dto.ReportParameterDTO;
import rs.formuvia.report.entity.Report;
import rs.formuvia.report.entity.ReportParameter;
import rs.formuvia.report.enums.ReportType;
import rs.formuvia.report.service.ReportService;
import rs.formuvia.report.utils.ParameterTablesNamesFinder;
import rs.formuvia.report.utils.UpdateReport;
import rs.formuvia.utils.StringUtils;

@Service
public class ReportServiceImpl implements ReportService {

	@Inject
	private DatabaseService databaseService;

	private ModelMapper modelMapper = new ModelMapper();

	@Context
	private HttpServletRequest httpServletRequest;

	@Override
	public DatabaseTable<ReportDTO> getTable(DatabaseParameter databaseParameter, UUID reportGroupId) {
		databaseParameter.getFilters().add(DatabaseFilter.valueOf("reportGroupId", reportGroupId.toString()));
		return databaseService.createTable(databaseParameter, ReportDTO.class);
	}

	@Override
	public ReportDTO getReport(UUID id) {
		Report report = this.databaseService.findById(id, Report.class);
		return modelMapper.map(report, ReportDTO.class);
	}

	@Override
	public ReportDTO updateReport(ReportDTO reportDTO) {

		UpdateReport updateReport = new UpdateReport(this.httpServletRequest, reportDTO);
		return this.databaseService.executeQuery(updateReport);
	}

	@Override
	public void deleteReport(UUID id) {
		Report report = this.databaseService.findById(id, Report.class);
		this.databaseService.delete(report);

	}

	@Override
	public DatabaseTable<ReportParameterDTO> getParameterTable(DatabaseParameter databaseParameter, UUID reportId) {
		databaseParameter.getFilters().add(DatabaseFilter.valueOf("reportId", reportId.toString()));
		return this.databaseService.createTable(databaseParameter, ReportParameterDTO.class);
	}

	@Override
	public ReportParameterDTO getParameterUpdate(ReportParameterDTO reportParameterDTO) {
		ReportParameter reportParameter = StringUtils.isNull(reportParameterDTO.getId()) ? new ReportParameter()
				: this.databaseService.findById(reportParameterDTO.getId(), ReportParameter.class);
		Report report = this.databaseService.findById(reportParameterDTO.getReportId(), Report.class);

		if (report.getType().equals(ReportType.EXECUTE) || report.getType().equals(ReportType.SQL)) {
			if (!UpdateColumnModel.COLUMN_NAME.matcher(reportParameterDTO.getCode()).matches())
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "invalidParameterCode",
						reportParameterDTO.getCode());
			Statement statement;
			try {
				statement = CCJSqlParserUtil.parse(report.getSqlQuery());
			} catch (JSQLParserException e) {
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "invalidQuery", report.getSqlQuery());
			}

			ParameterTablesNamesFinder parameterTablesNamesFinder = new ParameterTablesNamesFinder();
			parameterTablesNamesFinder.getTables(statement);
			if (!parameterTablesNamesFinder.getNames().contains(reportParameterDTO.getCode()))
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noParameterFoundInQuery",
						":" + reportParameterDTO.getCode());
		} else {
			JasperReport jasperReport = null;
			try {
				File file = DownloadModelFile.createFileFromPath(report.getJasterFile().getPath(), report.getId());
				jasperReport = ((JasperReport) JRLoader.loadObject(file));
			} catch (Exception ex) {
				throw new WebApplicationException(ex);
			}

			if (Arrays.asList(jasperReport.getParameters()).stream().filter(a -> !a.isSystemDefined())
					.filter(a -> a.getName().equals(reportParameterDTO.getCode())).count() == 0)
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noParameterFoundInJasper",
						":" + reportParameterDTO.getCode());
		}

		if (reportParameterDTO.getColumnType().equals(ColumnType.FILE))
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noColumnTypeAllowed", null);

		DatabaseParameter uniqueParameter = DatabaseParameter.singleFilter(
				new DatabaseFilter[] { DatabaseFilter.valueOf("reportId", reportParameterDTO.getReportId().toString()),
						DatabaseFilter.valueOf("code", reportParameterDTO.getCode()) });

		if (StringUtils.notNull(reportParameterDTO.getId()))
			uniqueParameter.getFilters().add(
					DatabaseFilter.valueOf("id", SearchOperation.NOT_EQUALS, reportParameterDTO.getId().toString()));

		if (this.databaseService.exists(uniqueParameter, ReportParameterDTO.class))
			throw new UniqueException(UniqueException.findFieldFromList(ReportParameterDTO.class, "code"),
					reportParameterDTO.getCode());

		uniqueParameter = DatabaseParameter.singleFilter(
				new DatabaseFilter[] { DatabaseFilter.valueOf("reportId", reportParameterDTO.getReportId().toString()),
						DatabaseFilter.valueOf("orderNumber", String.valueOf(reportParameterDTO.getOrderNumber())) });

		if (StringUtils.notNull(reportParameterDTO.getId()))
			uniqueParameter.getFilters().add(
					DatabaseFilter.valueOf("id", SearchOperation.NOT_EQUALS, reportParameterDTO.getId().toString()));

		if (this.databaseService.exists(uniqueParameter, ReportParameterDTO.class))
			throw new UniqueException(UniqueException.findFieldFromList(ReportParameterDTO.class, "orderNumber"),
					String.valueOf(reportParameterDTO.getOrderNumber()));

		if (StringUtils.hasText(reportParameterDTO.getDefaultValue()))
			UpdateColumnModel.checkDefaultValue(reportParameterDTO.getDefaultValue());

		if (StringUtils.hasText(reportParameterDTO.getListOfValues()))
			UpdateColumnModel.checkListOfValuesQuery(reportParameterDTO.getListOfValues());

		this.modelMapper.map(reportParameterDTO, reportParameter);

		reportParameter = this.databaseService.save(reportParameter);

		return modelMapper.map(reportParameter, ReportParameterDTO.class);
	}

	@Override
	public void getParameterDelete(UUID id) {
		ReportParameter reportParameter = this.databaseService.findById(id, ReportParameter.class);
		this.databaseService.delete(reportParameter);

	}

	@Override
	public Integer getParameterOrderNumber(UUID reportId) {
		DatabaseParameter databaseParameter = DatabaseParameter
				.singleFilter(new DatabaseFilter[] { DatabaseFilter.valueOf("reportId", reportId.toString()) });
		databaseParameter.getOrders().add(QueryDatabaseOrder.valueOf("orderNumber", Direction.DESC));

		List<ReportParameterDTO> list = this.databaseService.findAll(databaseParameter, ReportParameterDTO.class);

		return list.isEmpty() ? 1 : list.getFirst().getOrderNumber() + 1;
	}

}
