package rs.formuvia.database.utils;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import rs.formuvia.database.annotations.SkipColumn;
import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

@RequiredArgsConstructor
public class ExecuteNativeQueryImpl<C> implements ExecuteQuery<C> {

	private final String query;
	private final Map<Integer, Object> parameters;
	private final Class<C> resultClass;
	private DatabaseService databaseService = new DatabaseServiceImpl();

	@SuppressWarnings("unchecked")
	@Override
	public C execute(Connection connection) throws Exception {

		PreparedStatement preparedStatement = connection.prepareStatement(query);
		if (parameters != null) {
			Iterator<Integer> iterator = parameters.keySet().iterator();
			while (iterator.hasNext()) {
				Integer key = iterator.next();
				preparedStatement.setObject(key, parameters.get(key));
			}
		}
		ResultSet resultSet = preparedStatement.executeQuery();
		List<C> list = new ArrayList<>();
		while (resultSet.next()) {
			if (StaticData.allClasses.contains(resultClass)) {
				C object = resultClass.getConstructor().newInstance();
				int columnIndex = 0;
				for (Field field : StaticData.classFields.get(resultClass).stream()
						.filter(a -> !a.isAnnotationPresent(SkipColumn.class)).collect(Collectors.toList())) {
					columnIndex++;
					field.set(object,
							getValueFromResultSet(field.getType(), resultSet.getObject(columnIndex), connection));
				}
				list.add(object);
			} else {
				if (resultClass.isArray()) {
					Object[] objects = new Object[resultSet.getMetaData().getColumnCount()];
					for (int i = 1; i <= objects.length; i++) {
						objects[i - 1] = resultSet.getObject(i);

						if (StringUtils.isNull(objects[i - 1])) {
							continue;
						}

						if (objects[i - 1] instanceof java.sql.Date) {
							objects[i - 1] = ((java.sql.Date) objects[i - 1]).toLocalDate();
						}

						if (objects[i - 1] instanceof java.sql.Timestamp) {
							objects[i - 1] = ((java.sql.Timestamp) objects[i - 1]).toLocalDateTime();
						}

						if (objects[i - 1] instanceof java.sql.Time) {
							objects[i - 1] = ((java.sql.Time) objects[i - 1]).toLocalTime();
						}

					}
					list.add((C) objects);
				} else if (resultClass.equals(Object.class)) {
					list.add((C) resultSet.getObject(1));
				} else {
					C object = getValueFromResultSet(resultClass, resultSet.getObject(1), connection);
					list.add(object);
				}

			}
		}
		resultSet.close();
		preparedStatement.close();

		return (C) list;
	}

	public static <T> ColumnType findColumnType(Class<T> type) {
		ColumnType columnType = Arrays.asList(ColumnType.values()).stream().filter(a -> a.typeClass.equals(type))
				.findFirst().orElse(null);
		return columnType;
	}

	@SuppressWarnings("unchecked")
	private <T> T getValueFromResultSet(Class<T> type, Object value, Connection connection) {
		if (value == null) {
			return null;
		}
		ColumnType columnType = findColumnType(type);
		if (columnType == null) {
			if (type.isEnum()) {
				String valueString = value.toString();
				@SuppressWarnings({ "rawtypes" })
				Class<? extends Enum> enumClass = (Class<? extends Enum<?>>) type;
				Enum<? extends Enum<?>> enumValue = Enum.valueOf(enumClass, valueString);
				return (T) enumValue;
			} else {
				Object resultObject = this.databaseService.findById((UUID) value, type, connection);
				return (T) resultObject;
			}
		}
		switch (columnType) {
		case BIGDECIMAL:
			return (T) value;
		case BOOLEAN:
			return (T) Boolean.valueOf(value.toString());
		case INTEGER:
			Number number = (Number) value;
			return (T) Integer.valueOf(number.intValue());
		case LOCALDATE:
			java.sql.Date date = (Date) value;
			return (T) date.toLocalDate();
		case LOCALDATETIME:
			java.sql.Timestamp timestamp = (Timestamp) value;
			return (T) timestamp.toLocalDateTime();
		case LOCALTIME:
			java.sql.Time time = (java.sql.Time) value;
			return (T) time.toLocalTime();
		case LONG:
			number = (Number) value;
			return (T) Long.valueOf(number.longValue());
		case STRING:
			return (T) value.toString();
		case UUID:
			return (T) value;

		}

		return null;
	}

}
