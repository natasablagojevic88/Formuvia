package rs.formuvia.report.service.impl;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.UUID;

import org.jvnet.hk2.annotations.Service;
import org.modelmapper.ModelMapper;

import jakarta.inject.Inject;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.execute.Execute;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.database.enums.Direction;
import rs.formuvia.database.enums.SearchOperation;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.database.utils.QueryDatabaseOrder;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.exceptions.NotNullException;
import rs.formuvia.exceptions.UniqueException;
import rs.formuvia.model.utils.UpdateColumnModel;
import rs.formuvia.report.dto.ReportDTO;
import rs.formuvia.report.dto.ReportParameterDTO;
import rs.formuvia.report.entity.Report;
import rs.formuvia.report.entity.ReportParameter;
import rs.formuvia.report.enums.ReportType;
import rs.formuvia.report.service.ReportService;
import rs.formuvia.report.utils.ParameterTablesNamesFinder;
import rs.formuvia.utils.StringUtils;

@Service
public class ReportServiceImpl implements ReportService {

	@Inject
	private DatabaseService databaseService;

	private ModelMapper modelMapper = new ModelMapper();

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
		Report report = StringUtils.isNull(reportDTO.getId()) ? new Report()
				: this.databaseService.findById(reportDTO.getId(), Report.class);

		if (reportDTO.getType().equals(ReportType.EXECUTE) || reportDTO.getType().equals(ReportType.SQL)) {
			if (StringUtils.isNull(reportDTO.getSqlQuery()))
				throw new NotNullException(UniqueException.findFieldFromList(ReportDTO.class, "sqlQuery"));
		} else {
			reportDTO.setSqlQuery(null);
		}

		DatabaseParameter uniqueParameter = DatabaseParameter.singleFilter(
				new DatabaseFilter[] { DatabaseFilter.valueOf("reportGroupId", reportDTO.getReportGroupId().toString()),
						DatabaseFilter.valueOf("name", reportDTO.getName()) });

		if (StringUtils.notNull(reportDTO.getId()))
			uniqueParameter.getFilters()
					.add(DatabaseFilter.valueOf("id", SearchOperation.NOT_EQUALS, reportDTO.getId().toString()));

		if (this.databaseService.exists(uniqueParameter, ReportDTO.class))
			throw new UniqueException(UniqueException.findFieldFromList(ReportDTO.class, "name"), reportDTO.getName());

		checkSqlQuery(reportDTO.getType(), reportDTO.getSqlQuery());

		this.modelMapper.map(reportDTO, report);

		report = this.databaseService.save(report);

		return getReport(report.getId());
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

		if (!UpdateColumnModel.COLUMN_NAME.matcher(reportParameterDTO.getCode()).matches())
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "invalidParameterCode",
					reportParameterDTO.getCode());

		if (report.getType().equals(ReportType.EXECUTE) || report.getType().equals(ReportType.SQL)) {
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
