package rs.formuvia.report.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import rs.formuvia.common.service.ResourceBundleService;
import rs.formuvia.common.service.impl.ResourceBundleServiceImpl;
import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.database.utils.ExecuteNativeQueryImpl;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.report.dto.ReportResultColumnDTO;
import rs.formuvia.report.dto.ReportResultDTO;
import rs.formuvia.utils.StringUtils;

public class ExecuteReportPreview implements ExecuteQuery<ReportResultDTO> {
	private final HttpServletRequest httpServletRequest;
	private final String query;
	private final Map<Integer, Object> parameter;

	private ResourceBundleService resourceBundleService;
	private final String COLUMN_PREFIX = "k";

	public ExecuteReportPreview(HttpServletRequest httpServletRequest, String query, Map<Integer, Object> parameter) {
		this.httpServletRequest = httpServletRequest;
		this.query = query;
		this.parameter = parameter;
		this.resourceBundleService = new ResourceBundleServiceImpl(this.httpServletRequest);
	}

	@Override
	public ReportResultDTO execute(Connection connection) throws Exception {
		PreparedStatement preparedStatement = connection.prepareStatement(query);
		Iterator<Integer> itr = parameter.keySet().iterator();

		while (itr.hasNext()) {
			Integer key = itr.next();
			preparedStatement.setObject(key, parameter.get(key));
		}

		ResultSet resultSet = preparedStatement.executeQuery();
		ReportResultDTO reportResultDTO = new ReportResultDTO();
		for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
			ReportResultColumnDTO reportResultColumnDTO = new ReportResultColumnDTO();
			reportResultColumnDTO.setCode(COLUMN_PREFIX + i);
			reportResultColumnDTO.setName(this.resourceBundleService.getText(resultSet.getMetaData().getColumnName(i)));

			if (StringUtils.hasText(resultSet.getMetaData().getColumnClassName(i))) {
				Class<?> resultClass = Class.forName(resultSet.getMetaData().getColumnClassName(i));

				if (resultClass.equals(java.sql.Date.class))
					reportResultColumnDTO.setColumnType(ColumnType.LOCALDATE);
				else if (resultClass.equals(java.sql.Time.class))
					reportResultColumnDTO.setColumnType(ColumnType.LOCALTIME);
				else if (resultClass.equals(java.sql.Timestamp.class))
					reportResultColumnDTO.setColumnType(ColumnType.LOCALDATETIME);
				else
					reportResultColumnDTO.setColumnType(ExecuteNativeQueryImpl.findColumnType(resultClass));

			}

			reportResultDTO.getColumns().add(reportResultColumnDTO);
		}

		while (resultSet.next()) {
			LinkedHashMap<String, Object> object = new LinkedHashMap<>();

			for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
				Object value = resultSet.getObject(i);
				if (StringUtils.isNull(value)) {
					object.put(COLUMN_PREFIX + i, null);
					continue;
				}
				ReportResultColumnDTO column = reportResultDTO.getColumns().get(i - 1);

				switch (column.getColumnType()) {
				case LOCALDATE:
					object.put(COLUMN_PREFIX + i, ((java.sql.Date) value).toLocalDate());
					break;
				case LOCALDATETIME:
					object.put(COLUMN_PREFIX + i, ((java.sql.Timestamp) value).toLocalDateTime());
					break;
				case LOCALTIME:
					object.put(COLUMN_PREFIX + i, ((java.sql.Time) value).toLocalTime());
					break;
				case UUID, BIGDECIMAL, BOOLEAN, INTEGER, LONG, STRING:
					object.put(COLUMN_PREFIX + i, value);
					break;
				case FILE:
					break;

				}
			}
			reportResultDTO.getList().add(object);
		}

		resultSet.close();
		preparedStatement.close();
		return reportResultDTO;
	}

}
