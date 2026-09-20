package rs.formuvia.database.utils;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import rs.formuvia.common.service.impl.AppStartUpImpl;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.database.annotations.InitSort;
import rs.formuvia.database.annotations.SkipColumn;
import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.database.enums.Direction;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.SqlQueryWriterService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.service.impl.SqlQueryWriterServiceImpl;
import rs.formuvia.utils.StaticData;

@RequiredArgsConstructor
public class GenerateQueryFromDTO<C> implements ExecuteQuery<C> {

	private final DatabaseParameter databaseParameter;
	private final Class<C> resultClass;
	private final static String allBigLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private SqlQueryWriterService sqlQueryWriterService = new SqlQueryWriterServiceImpl();
	private DatabaseService databaseService = new DatabaseServiceImpl();

	@SuppressWarnings("unchecked")
	@Override
	public C execute(Connection connection) throws Exception {

		QueryTableInfo queryTableInfo = createQueryTableInfo(this.resultClass);
		DatabaseParameter databaseParameterFinal = databaseParameter == null ? new DatabaseParameter()
				: databaseParameter;
		if (databaseParameterFinal.getOrders().isEmpty()) {
			List<Field> fieldWithInitOrder = StaticData.classFields.get(resultClass).stream()
					.filter(a -> a.isAnnotationPresent(InitSort.class)).collect(Collectors.toList());
			List<InitSortData> initSortDatas = new ArrayList<>();
			for (Field field : fieldWithInitOrder) {
				InitSort initSort = field.getAnnotation(InitSort.class);
				initSortDatas.add(new InitSortData(initSort.orderNumber(), field.getName(), initSort.direction()));
			}
			initSortDatas = initSortDatas.stream().sorted(Comparator.comparing(InitSortData::getOrderNumber))
					.collect(Collectors.toList());
			databaseParameterFinal.setOrders(initSortDatas.stream()
					.map(a -> new QueryDatabaseOrder(a.getFieldName(), a.getDirection())).collect(Collectors.toList()));

		}

		fillColumnType(databaseParameterFinal, resultClass);

		String query = sqlQueryWriterService.createSelectQuery(queryTableInfo, databaseParameterFinal);
		Map<Integer, Object> parameters = sqlQueryWriterService.createParameters(databaseParameterFinal.getFilters());
		return (C) databaseService.executeNativeQuery(query, parameters, resultClass, connection);
	}

	public static void fillColumnType(DatabaseParameter databaseParameter, Class<?> resultClass) {
		for (DatabaseFilter databaseFilter : databaseParameter.getFilters()) {
			Field field = findFieldFromStaticList(resultClass, databaseFilter.getField());
			databaseFilter.setColumnType(ExecuteNativeQueryImpl.findColumnType(field.getType()));
		}
	}

	public static QueryTableInfo createQueryTableInfo(Class<?> resultClass) {
		List<Field> fields = StaticData.classFields.get(resultClass).stream()
				.filter(a -> !a.isAnnotationPresent(SkipColumn.class)).collect(Collectors.toList());
		String tableName = findTableName(resultClass);
		QueryTableInfo queryTableInfo = new QueryTableInfo();
		queryTableInfo.setName(tableName);

		Class<?> entityClass = findEntityClass(resultClass);
		List<Field> fieldsEntity = StaticData.classFields.get(entityClass).stream().collect(Collectors.toList());

		for (Field field : fields) {
			String fieldName = field.getName();
			QueryColumnInfo columnInfo = new QueryColumnInfo();
			List<String> paths = new ArrayList<>();
			List<String> tables = new ArrayList<>();
			if (checkFieldExists(fieldsEntity, fieldName)) {
				Field foundField = findFieldFromList(fieldsEntity, fieldName);
				columnInfo.setColumnName(AppStartUpImpl.findColumnName(foundField));
				columnInfo.setColumnType(findColumnType(field.getType()));
				columnInfo.setFieldName(fieldName);
			} else {
				int startIndex = 0;
				List<Integer> indexes = new ArrayList<>();
				for (int i = 0; i < fieldName.length(); i++) {
					if (!allBigLetters.contains(String.valueOf(fieldName.charAt(i)))) {
						continue;
					}
					indexes.add(i);
				}
				indexes.add(fieldName.length());
				Class<?> lastClass = entityClass;
				List<String> fieldNamesAll = new ArrayList<>();
				for (Integer index : indexes) {
					fieldNamesAll.add(fieldName.substring(startIndex, index));
					startIndex = index;
				}
				String fieldToFind = "";
				int countIndex = 0;

				for (String findFieldName : fieldNamesAll) {
					countIndex++;
					List<Field> findFieldList = StaticData.classFields.get(lastClass).stream()
							.collect(Collectors.toList());
					fieldToFind += findFieldName;
					if (!checkFieldExists(findFieldList, normalizeFieldName(fieldToFind))) {
						continue;
					}
					Field foundField = findFieldFromList(findFieldList, normalizeFieldName(fieldToFind));
					lastClass = foundField.getType();
					fieldToFind = "";

					if (lastClass.isAnnotationPresent(Table.class)) {
						paths.add(AppStartUpImpl.findColumnName(foundField));
						tables.add(lastClass.getAnnotation(Table.class).name());
					}

					if (countIndex == fieldNamesAll.size()) {
						columnInfo.setColumnName(AppStartUpImpl.findColumnName(foundField));
						columnInfo.setColumnType(findColumnType(foundField.getType()));
						columnInfo.setFieldName(fieldName);
					}

				}
			}
			columnInfo.setPaths(paths);
			columnInfo.setTableName(tables);
			queryTableInfo.getColumns().add(columnInfo);
		}
		return queryTableInfo;

	}

	private static ColumnType findColumnType(Class<?> typeClass) {
		ColumnType columnInfo = ExecuteNativeQueryImpl.findColumnType(typeClass);
		if (columnInfo == null)
			return ColumnType.STRING;

		return columnInfo;
	}

	private static Field findFieldFromList(List<Field> fields, String fieldName) {
		return fields.stream().filter(a -> a.getName().equals(fieldName)).findFirst().orElse(null);
	}

	private static Field findFieldFromStaticList(Class<?> resultClass, String fieldName) {
		return StaticData.classFields.get(resultClass).stream().filter(a -> a.getName().equals(fieldName)).findFirst()
				.orElse(null);
	}

	private static String normalizeFieldName(String fieldName) {
		String result = String.valueOf(fieldName.charAt(0)).toLowerCase();
		result += fieldName.substring(1, fieldName.length());
		return result;
	}

	private static Boolean checkFieldExists(List<Field> fields, String fieldName) {
		return fields.stream().filter(a -> a.getName().equals(fieldName)).count() != 0;
	}

	public static String findTableName(Class<?> inClass) {
		if (inClass.isAnnotationPresent(Table.class)) {
			return inClass.getAnnotation(Table.class).name();
		} else {
			return inClass.getAnnotation(EntityClass.class).value().getAnnotation(Table.class).name();
		}
	}

	private static Class<?> findEntityClass(Class<?> inClass) {
		if (inClass.isAnnotationPresent(Table.class)) {
			return inClass;
		} else {
			return inClass.getAnnotation(EntityClass.class).value();
		}
	}

}

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
class InitSortData {
	private Integer orderNumber;

	private String fieldName;

	private Direction direction;
}
