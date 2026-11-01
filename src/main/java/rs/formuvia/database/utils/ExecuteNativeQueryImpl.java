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
import rs.formuvia.model.entity.ModelFile;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

@RequiredArgsConstructor
public class ExecuteNativeQueryImpl<C> implements ExecuteQuery<C> {

	private final String query;
	private final Map<Integer, Object> parameters;
	private final Class<C> resultClass;
	private DatabaseService databaseService = new DatabaseServiceImpl();

	private static final String ARRAY_TEXT_TYPE = "text";
	private static final String ARRAY_UUID_TYPE = "uuid";
	private static final String ARRAY_INTEGER_TYPE = "integer";
	private static final String ARRAY_BIGINT_TYPE = "bigint";

	@SuppressWarnings("unchecked")
	@Override
	public C execute(Connection connection) throws Exception {

		PreparedStatement preparedStatement = connection.prepareStatement(query);
		if (parameters != null) {
			Iterator<Integer> iterator = parameters.keySet().iterator();
			while (iterator.hasNext()) {
				Integer key = iterator.next();
				Object value = parameters.get(key);
				if (value == null) {
					preparedStatement.setObject(key, null);
				} else {
					if (value instanceof Object[]) {
						Object[] list = (Object[]) value;
						Object first = list.length == 0 ? null : list[0];

						String type = ARRAY_TEXT_TYPE;
						if (first != null) {
							if (first instanceof UUID)
								type = ARRAY_UUID_TYPE;
							else if (first instanceof Integer)
								type = ARRAY_INTEGER_TYPE;
							else if (first instanceof Long)
								type = ARRAY_BIGINT_TYPE;
						}

						preparedStatement.setObject(key, connection.createArrayOf(type, list));

					} else {
						preparedStatement.setObject(key, value);
					}
				}

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
						objects[i - 1] = getObjectValue(objects[i - 1]);
					}
					list.add((C) objects);
				} else if (resultClass.equals(Object.class)) {
					list.add((C) getObjectValue(resultSet.getObject(1)));
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

	private Object getObjectValue(Object inObject) {
		if (StringUtils.isNull(inObject)) {
			return null;
		}

		Object object = inObject;

		if (inObject instanceof java.sql.Date) {
			object = ((java.sql.Date) inObject).toLocalDate();
		}

		if (inObject instanceof java.sql.Timestamp) {
			object = ((java.sql.Timestamp) inObject).toLocalDateTime();
		}

		if (inObject instanceof java.sql.Time) {
			object = ((java.sql.Time) inObject).toLocalTime();
		}

		return object;
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
		case FILE:
			Object resultObject = this.databaseService.findById((UUID) value, ModelFile.class, connection);
			return (T) resultObject;

		}

		return null;
	}

}
