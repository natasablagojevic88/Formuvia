package rs.formuvia.database.service.impl;

import java.io.BufferedWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.Service;

import jakarta.ws.rs.WebApplicationException;
import rs.formuvia.database.enums.SearchOperation;
import rs.formuvia.database.service.SqlQueryWriterService;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.QueryColumnFinalInfo;
import rs.formuvia.database.utils.QueryColumnInfo;
import rs.formuvia.database.utils.QueryDatabaseOrder;
import rs.formuvia.database.utils.QueryLeftJoin;
import rs.formuvia.database.utils.QueryTableFinalInfo;
import rs.formuvia.database.utils.QueryTableInfo;
import rs.formuvia.utils.StringUtils;

@Service
public class SqlQueryWriterServiceImpl implements SqlQueryWriterService {
	private final String mainTableAlias = "a";
	private final String joinTableAlias = "t";
	public static final String defaultIdColumn = "id";
	private final String selectCountPart = "select count(*)";

	private Logger logger = LogManager.getLogger(getClass());

	private QueryTableFinalInfo createQueryTableFinalInfo(QueryTableInfo queryTableInfo) {
		QueryTableFinalInfo queryTableFinalInfo = new QueryTableFinalInfo();
		queryTableFinalInfo.setName(queryTableInfo.getName());
		createLeftJoin(queryTableInfo, queryTableFinalInfo);
		createColumns(queryTableInfo, queryTableFinalInfo);
		return queryTableFinalInfo;
	}

	private void createLeftJoin(QueryTableInfo queryTableInfo, QueryTableFinalInfo queryTableFinalInfo) {
		List<QueryLeftJoin> leftJoin = new ArrayList<>();
		int leftJoinCount = 0;
		for (QueryColumnInfo columnInfo : queryTableInfo.getColumns()) {
			if (columnInfo.getPaths().isEmpty()) {
				continue;
			}
			String pathName = "";
			int index = -1;
			for (String path : columnInfo.getPaths()) {
				index++;
				String previousPathName = new String(pathName.getBytes());
				pathName += "/" + path;
				final String pathNameFinal = new String(pathName.getBytes());
				String tableName = columnInfo.getTableName().get(index);

				if (leftJoin.stream().filter(a -> a.getPath().equals(pathNameFinal)).count() != 0) {
					continue;
				}

				leftJoinCount++;
				QueryLeftJoin queryLeftJoin = new QueryLeftJoin();
				queryLeftJoin.setPath(pathName);
				queryLeftJoin.setPrefix(joinTableAlias + leftJoinCount);
				String referenceTable = "";
				if (index == 0) {
					referenceTable = mainTableAlias + "." + path;
				} else {
					QueryLeftJoin previousJoin = leftJoin.stream().filter(a -> a.getPath().equals(previousPathName))
							.findFirst().get();
					referenceTable = previousJoin.getPrefix() + "." + path;
				}
				queryLeftJoin.setReferenceTable(referenceTable);
				queryLeftJoin.setTableName(tableName);
				leftJoin.add(queryLeftJoin);
			}

		}
		queryTableFinalInfo.setLeftJoins(leftJoin);
	}

	private void createColumns(QueryTableInfo queryTableInfo, QueryTableFinalInfo queryTableFinalInfo) {
		for (QueryColumnInfo columnInfo : queryTableInfo.getColumns()) {
			QueryColumnFinalInfo queryColumnFinalInfo = new QueryColumnFinalInfo();
			queryColumnFinalInfo.setColumnName(columnInfo.getColumnName());
			queryColumnFinalInfo.setFieldName(columnInfo.getFieldName());
			queryColumnFinalInfo
					.setColumnPrefix(columnInfo.getPaths().isEmpty() ? mainTableAlias
							: queryTableFinalInfo
									.getLeftJoins().stream().filter(
											a -> a.getPath()
													.equals(columnInfo.getPaths().stream().map(b -> "/" + b)
															.collect(Collectors.joining())))
									.findFirst().get().getPrefix());
			queryTableFinalInfo.getColumns().add(queryColumnFinalInfo);
		}
	}

	@Override
	public String createSelectQuery(QueryTableInfo queryTableInfo, DatabaseParameter databaseParameter) {
		QueryTableFinalInfo queryTableFinalInfo = createQueryTableFinalInfo(queryTableInfo);
		String query = "";
		query += createSelectPart(queryTableFinalInfo.getColumns());
		query += "\n";
		query += createFromPart(queryTableFinalInfo.getName(), queryTableFinalInfo.getLeftJoins());
		List<DatabaseFilter> databaseFilters = clearDatabaseFilters(databaseParameter.getFilters());
		if (!databaseFilters.isEmpty()) {
			query += "\n";
			query += createWherePart(queryTableFinalInfo.getColumns(), databaseFilters);
		}
		if (!databaseParameter.getOrders().isEmpty()) {
			query += "\n";
			query += createOrderPart(queryTableFinalInfo.getColumns(), databaseParameter.getOrders());
		}
		query += "\n";
		query += createPagingPart(databaseParameter.getPageIndex(), databaseParameter.getPageSize());

		return query;
	}

	private String createSelectPart(List<QueryColumnFinalInfo> columns) {
		try {
			StringWriter stringWriter = new StringWriter();
			BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);
			bufferedWriter.write("SELECT");
			int index = 0;
			for (QueryColumnFinalInfo columnFinalInfo : columns) {
				index++;
				bufferedWriter.newLine();
				bufferedWriter.write(columnFinalInfo.getColumnPrefix() + "." + columnFinalInfo.getColumnName());
				if (index != columns.size())
					bufferedWriter.write(",");
			}

			bufferedWriter.close();
			return stringWriter.toString();
		} catch (Exception e) {
			this.logger.error(e.getMessage(), e);
			throw new WebApplicationException(e);
		}
	}

	private String createFromPart(String tableName, List<QueryLeftJoin> leftJoins) {
		try {
			StringWriter stringWriter = new StringWriter();
			BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);
			bufferedWriter.write("FROM");
			bufferedWriter.newLine();
			bufferedWriter.write(tableName + " " + mainTableAlias);

			for (QueryLeftJoin leftJoin : leftJoins) {
				bufferedWriter.newLine();
				bufferedWriter.write("LEFT JOIN ");
				bufferedWriter.write(leftJoin.getTableName() + " " + leftJoin.getPrefix());
				bufferedWriter.newLine();
				bufferedWriter.write("ON ");
				bufferedWriter.write(leftJoin.getReferenceTable());
				bufferedWriter.write("=");
				bufferedWriter.write(leftJoin.getPrefix() + "." + defaultIdColumn);
			}

			bufferedWriter.close();
			return stringWriter.toString();
		} catch (Exception e) {
			this.logger.error(e.getMessage(), e);
			throw new WebApplicationException(e);
		}
	}

	public static List<DatabaseFilter> clearDatabaseFilters(List<DatabaseFilter> databaseFilters) {
		List<DatabaseFilter> databaseFiltersFiltered = new ArrayList<>();
		for (DatabaseFilter databaseFilter : databaseFilters) {

			SearchOperation searchOperation = databaseFilter.getSearchOperation();

			switch (searchOperation) {
			case IS_NULL, IS_NOT_NULL: {
				databaseFiltersFiltered.add(databaseFilter);
				break;
			}
			case BETWEEN: {
				if (StringUtils.hasText(databaseFilter.getField1())
						&& StringUtils.hasText(databaseFilter.getField2())) {
					databaseFiltersFiltered.add(databaseFilter);
				}
				break;
			}
			default: {
				if (StringUtils.hasText(databaseFilter.getField1())) {
					databaseFiltersFiltered.add(databaseFilter);
				}
				break;
			}
			}
		}
		return databaseFiltersFiltered;
	}

	private String createWherePart(List<QueryColumnFinalInfo> columns, List<DatabaseFilter> databaseFilters) {
		try {
			StringWriter stringWriter = new StringWriter();
			BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);
			bufferedWriter.write("WHERE");
			int index = 0;
			for (DatabaseFilter databaseFilter : databaseFilters) {
				index++;
				bufferedWriter.newLine();

				if (index != 1)
					bufferedWriter.write("AND ");

				QueryColumnFinalInfo columnFinalInfo = findColumnFromList(columns, databaseFilter.getField());
				String columnName = columnFinalInfo.getColumnPrefix() + "." + columnFinalInfo.getColumnName();
				switch (databaseFilter.getSearchOperation()) {
				case BETWEEN:
					bufferedWriter.write(columnName);
					bufferedWriter.write(" ");
					bufferedWriter.write("BETWEEN ? and ?");
					break;
				case CONTAINS:
					bufferedWriter.write("lower(" + columnName + ")");
					bufferedWriter.write(" ");
					bufferedWriter.write("like lower('%'||?||'%')");
					break;
				case ENDS_WITH:
					bufferedWriter.write("lower(" + columnName + ")");
					bufferedWriter.write(" ");
					bufferedWriter.write("like lower('%'||?)");
					break;
				case EQUALS:
					bufferedWriter.write(columnName);
					bufferedWriter.write(" ");
					bufferedWriter.write("= ?");
					break;
				case GREATER_THEN:
					bufferedWriter.write(columnName);
					bufferedWriter.write(" ");
					bufferedWriter.write("> ?");
					break;
				case IS_NOT_NULL:
					bufferedWriter.write(columnName);
					bufferedWriter.write(" is not null");
					break;
				case IS_NULL:
					bufferedWriter.write(columnName);
					bufferedWriter.write(" is null");
					break;
				case LESS_THEN:
					bufferedWriter.write(columnName);
					bufferedWriter.write(" ");
					bufferedWriter.write("< ?");
					break;
				case NOT_EQUALS:
					bufferedWriter.write(columnName);
					bufferedWriter.write(" ");
					bufferedWriter.write("!= ?");
					break;
				case STARTS_WITH:
					bufferedWriter.write("lower(" + columnName + ")");
					bufferedWriter.write(" ");
					bufferedWriter.write("like lower(?||'%')");
					break;
				case GREATER_THEN_OR_EQUALS:
					bufferedWriter.write(columnName);
					bufferedWriter.write(" ");
					bufferedWriter.write(">= ?");
					break;
				case LESS_THEN_OR_EQUALS:
					bufferedWriter.write(columnName);
					bufferedWriter.write(" ");
					bufferedWriter.write("<= ?");
					break;

				}

			}
			bufferedWriter.close();
			return stringWriter.toString();
		} catch (Exception e) {
			this.logger.error(e.getMessage(), e);
			throw new WebApplicationException(e);
		}
	}

	private QueryColumnFinalInfo findColumnFromList(List<QueryColumnFinalInfo> columns, String fieldName) {
		return columns.stream().filter(a -> a.getFieldName().equals(fieldName)).findFirst().get();
	}

	private String createOrderPart(List<QueryColumnFinalInfo> columns, List<QueryDatabaseOrder> orders) {
		try {
			StringWriter stringWriter = new StringWriter();
			BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);
			bufferedWriter.write("ORDER BY");
			int index = 0;
			for (QueryDatabaseOrder order : orders) {
				index++;
				QueryColumnFinalInfo column = findColumnFromList(columns, order.getFieldName());
				String columnName = column.getColumnPrefix() + "." + column.getColumnName();
				bufferedWriter.newLine();

				bufferedWriter.write(columnName);
				bufferedWriter.write(" ");
				bufferedWriter.write(order.getDirection().name());

				if (index != orders.size())
					bufferedWriter.write(",");

			}

			bufferedWriter.close();
			return stringWriter.toString();
		} catch (Exception e) {
			this.logger.error(e.getMessage(), e);
			throw new WebApplicationException(e);
		}
	}

	private String createPagingPart(Integer pageIndex, Integer pageSize) {
		try {
			StringWriter stringWriter = new StringWriter();
			BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);
			bufferedWriter.write("LIMIT ");
			bufferedWriter.write(String.valueOf(pageSize));
			bufferedWriter.write(" OFFSET ");
			Integer offset = pageSize * pageIndex;
			bufferedWriter.write(String.valueOf(offset));

			bufferedWriter.close();
			return stringWriter.toString();
		} catch (Exception e) {
			this.logger.error(e.getMessage(), e);
			throw new WebApplicationException(e);
		}
	}

	@Override
	public Map<Integer, Object> createParameters(List<DatabaseFilter> databaseFilters) {
		Map<Integer, Object> parameters = new HashMap<>();
		int count = 0;
		List<DatabaseFilter> databaseFiltersFilteed = clearDatabaseFilters(databaseFilters);
		for (DatabaseFilter databaseFilter : databaseFiltersFilteed) {
			Object[] objects = null;
			switch (databaseFilter.getColumnType()) {
			case BIGDECIMAL:
				objects = createBigDecimalParameters(databaseFilter);
				break;
			case BOOLEAN:
				objects = createBooleanParameters(databaseFilter);
				break;
			case INTEGER:
				objects = createIntegerParameters(databaseFilter);
				break;
			case LOCALDATE:
				objects = createLocalDateParameters(databaseFilter);
				break;
			case LOCALDATETIME:
				objects = createLocalDateTimeParameters(databaseFilter);
				break;
			case LONG:
				objects = createLongParameters(databaseFilter);
				break;
			case STRING:
				objects = createStringParameters(databaseFilter);
				break;
			case UUID:
				objects = createUUIDParameters(databaseFilter);
				break;

			}

			for (Object object : objects) {
				count++;
				parameters.put(count, object);
			}
		}

		return parameters;
	}

	private Object[] createIntegerParameters(DatabaseFilter databaseFilter) {
		Object[] objects = null;
		switch (databaseFilter.getSearchOperation()) {
		case BETWEEN: {
			objects = new Object[2];
			objects[0] = Integer.valueOf(databaseFilter.getField1());
			objects[1] = Integer.valueOf(databaseFilter.getField2());
			break;
		}
		case IS_NULL, IS_NOT_NULL: {
			objects = new Object[0];
			break;
		}
		default: {
			objects = new Object[1];
			objects[0] = Integer.valueOf(databaseFilter.getField1());
			break;
		}
		}

		return objects;
	}

	private Object[] createLongParameters(DatabaseFilter databaseFilter) {
		Object[] objects = null;
		switch (databaseFilter.getSearchOperation()) {
		case BETWEEN: {
			objects = new Object[2];
			objects[0] = Long.valueOf(databaseFilter.getField1());
			objects[1] = Long.valueOf(databaseFilter.getField2());
			break;
		}
		case IS_NULL, IS_NOT_NULL: {
			objects = new Object[0];
			break;
		}
		default: {
			objects = new Object[1];
			objects[0] = Long.valueOf(databaseFilter.getField1());
			break;
		}
		}

		return objects;
	}

	private Object[] createBigDecimalParameters(DatabaseFilter databaseFilter) {
		Object[] objects = null;

		switch (databaseFilter.getSearchOperation()) {
		case BETWEEN: {
			objects = new Object[2];
			objects[0] = new BigDecimal(databaseFilter.getField1());
			objects[1] = new BigDecimal(databaseFilter.getField2());
			break;
		}
		case IS_NULL, IS_NOT_NULL: {
			objects = new Object[0];
			break;
		}
		default: {
			objects = new Object[1];
			objects[0] = new BigDecimal(databaseFilter.getField1());
			break;
		}
		}

		return objects;
	}

	private Object[] createLocalDateParameters(DatabaseFilter databaseFilter) {
		Object[] objects = null;
		switch (databaseFilter.getSearchOperation()) {
		case BETWEEN: {
			objects = new Object[2];
			objects[0] = LocalDate.parse(databaseFilter.getField1());
			objects[1] = LocalDate.parse(databaseFilter.getField2());
			break;
		}
		case IS_NULL, IS_NOT_NULL: {
			objects = new Object[0];
			break;
		}
		default: {
			objects = new Object[1];
			objects[0] = LocalDate.parse(databaseFilter.getField1());
			break;
		}
		}

		return objects;
	}

	private Object[] createLocalDateTimeParameters(DatabaseFilter databaseFilter) {
		Object[] objects = null;
		switch (databaseFilter.getSearchOperation()) {
		case BETWEEN: {
			objects = new Object[2];
			objects[0] = LocalDateTime.parse(databaseFilter.getField1());
			objects[1] = LocalDateTime.parse(databaseFilter.getField2());
			break;
		}
		case IS_NULL, IS_NOT_NULL: {
			objects = new Object[0];
			break;
		}
		default: {
			objects = new Object[1];
			objects[0] = LocalDateTime.parse(databaseFilter.getField1());
			break;
		}
		}

		return objects;
	}

	private Object[] createBooleanParameters(DatabaseFilter databaseFilter) {
		Object[] objects = null;
		switch (databaseFilter.getSearchOperation()) {
		case IS_NULL, IS_NOT_NULL: {
			objects = new Object[0];
			break;
		}
		default: {
			objects = new Object[1];
			objects[0] = Boolean.valueOf(databaseFilter.getField1());
			break;
		}
		}

		return objects;
	}

	private Object[] createStringParameters(DatabaseFilter databaseFilter) {
		Object[] objects = null;
		switch (databaseFilter.getSearchOperation()) {
		case IS_NULL, IS_NOT_NULL: {
			objects = new Object[0];
			break;
		}
		default: {
			objects = new Object[1];
			objects[0] = databaseFilter.getField1();
			break;
		}
		}

		return objects;
	}

	private Object[] createUUIDParameters(DatabaseFilter databaseFilter) {
		Object[] objects = null;
		switch (databaseFilter.getSearchOperation()) {
		case IS_NULL, IS_NOT_NULL: {
			objects = new Object[0];
			break;
		}
		default: {
			objects = new Object[1];
			objects[0] = UUID.fromString(databaseFilter.getField1());
			break;
		}
		}

		return objects;
	}

	@Override
	public String createTotalQuery(QueryTableInfo queryTableInfo, DatabaseParameter databaseParameter) {
		QueryTableFinalInfo queryTableFinalInfo = createQueryTableFinalInfo(queryTableInfo);
		String query = "";
		query += selectCountPart;
		query += "\n";
		query += createFromPart(queryTableFinalInfo.getName(), queryTableFinalInfo.getLeftJoins());
		List<DatabaseFilter> databaseFilters = clearDatabaseFilters(databaseParameter.getFilters());
		if (!databaseFilters.isEmpty()) {
			query += "\n";
			query += createWherePart(queryTableFinalInfo.getColumns(), databaseFilters);
		}

		return query;
	}

	private String[] columnsWithoutId(String[] fieldArray) {
		return Arrays.asList(fieldArray).stream().filter(a -> !a.equals(defaultIdColumn)).collect(Collectors.toList())
				.toArray(String[]::new);
	}

	@Override
	public String insertQuery(String[] fieldArray, String tableName) {

		try {

			StringWriter stringWriter = new StringWriter();
			BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);
			String[] fieldArrayWithoutId = columnsWithoutId(fieldArray);
			String[] parameterArray = createPararameterArray(fieldArrayWithoutId);

			bufferedWriter.write("INSERT INTO");
			bufferedWriter.newLine();
			bufferedWriter.write(tableName);
			bufferedWriter.newLine();
			bufferedWriter.write("(");
			bufferedWriter.newLine();
			bufferedWriter.write(StringUtils.createStringArrayWithDelimiter(fieldArrayWithoutId, ",\n"));
			bufferedWriter.newLine();
			bufferedWriter.write(")");
			bufferedWriter.newLine();
			bufferedWriter.write("values");
			bufferedWriter.newLine();
			bufferedWriter.write("(");
			bufferedWriter.newLine();
			bufferedWriter.write(StringUtils.createStringArrayWithDelimiter(parameterArray, ",\n"));
			bufferedWriter.newLine();
			bufferedWriter.write(")");
			bufferedWriter.newLine();
			bufferedWriter.write("RETURNING");
			bufferedWriter.newLine();
			bufferedWriter.write(StringUtils.createStringArray(fieldArray));

			bufferedWriter.close();
			return stringWriter.toString();
		} catch (Exception e) {
			this.logger.error(e.getMessage(), e);
			throw new WebApplicationException(e);
		}

	}

	private String[] createPararameterArray(String[] fieldArray) {
		return Arrays.asList(fieldArray).stream().map(a -> "?").toArray(String[]::new);
	}

	@Override
	public String updateQuery(String[] fieldArray, String tableName) {

		try {

			StringWriter stringWriter = new StringWriter();
			BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);
			String[] fieldArrayWithoutId = columnsWithoutId(fieldArray);
			String[] parameterArray = createPararameterArray(fieldArrayWithoutId);

			bufferedWriter.write("UPDATE ");
			bufferedWriter.newLine();
			bufferedWriter.write(tableName);
			bufferedWriter.newLine();
			bufferedWriter.write("SET");
			bufferedWriter.newLine();
			if (fieldArrayWithoutId.length > 1) {
				bufferedWriter.write("(");
				bufferedWriter.newLine();
			}
			bufferedWriter.write(StringUtils.createStringArrayWithDelimiter(fieldArrayWithoutId, ",\n"));
			bufferedWriter.newLine();
			if (fieldArrayWithoutId.length > 1) {
				bufferedWriter.write(")");
				bufferedWriter.newLine();
			}
			bufferedWriter.write("=");
			bufferedWriter.newLine();
			if (fieldArrayWithoutId.length > 1) {
				bufferedWriter.write("(");
				bufferedWriter.newLine();
			}
			bufferedWriter.write(StringUtils.createStringArrayWithDelimiter(parameterArray, ",\n"));
			bufferedWriter.newLine();
			if (fieldArrayWithoutId.length > 1) {
				bufferedWriter.write(")");
				bufferedWriter.newLine();
			}
			bufferedWriter.write("WHERE " + defaultIdColumn + " = ?");
			bufferedWriter.newLine();
			bufferedWriter.write("RETURNING");
			bufferedWriter.newLine();
			bufferedWriter.write(StringUtils.createStringArray(fieldArray));

			bufferedWriter.close();
			return stringWriter.toString();
		} catch (Exception e) {
			this.logger.error(e.getMessage(), e);
			throw new WebApplicationException(e);
		}

	}

	@Override
	public String deleteQuery(String tableName) {
		try {
			StringWriter stringWriter = new StringWriter();
			BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);
			bufferedWriter.write("DELETE FROM ");
			bufferedWriter.write(tableName);
			bufferedWriter.newLine();
			bufferedWriter.write("WHERE id=?");

			bufferedWriter.close();
			return stringWriter.toString();
		} catch (Exception e) {
			this.logger.error(e.getMessage(), e);
			throw new WebApplicationException(e);
		}
	}
}
