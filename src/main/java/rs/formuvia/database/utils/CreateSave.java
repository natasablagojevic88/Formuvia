package rs.formuvia.database.utils;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.Table;
import lombok.RequiredArgsConstructor;
import rs.formuvia.common.service.impl.AppStartUpImpl;
import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.SqlQueryWriterService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.service.impl.SqlQueryWriterServiceImpl;
import rs.formuvia.utils.StaticData;

@RequiredArgsConstructor
public class CreateSave<C> implements ExecuteQuery<C> {
	private final C entity;

	private DatabaseService databaseService = new DatabaseServiceImpl();
	private SqlQueryWriterService sqlQueryWriterService = new SqlQueryWriterServiceImpl();

	@Override
	public C execute(Connection connection) throws Exception {

		Boolean insert = true;
		@SuppressWarnings("unchecked")
		Class<C> entityClass = (Class<C>) entity.getClass();

		UUID id = findId(entity);

		if (id != null) {
			this.databaseService.findById(id, entity.getClass(), connection);
			insert = false;
		}

		String query = null;
		Map<Integer, Object> parameters = createParameters(this.entity);
		String[] fieldsForUpdate = fieldsForUpdate(entity);
		if (insert) {
			query = this.sqlQueryWriterService.insertQuery(fieldsForUpdate, findTableName(entity));
		} else {
			query = this.sqlQueryWriterService.updateQuery(fieldsForUpdate, findTableName(entity));
		}

		C newObject = this.databaseService.executeNativeQuery(query, parameters, entityClass, connection).getFirst();

		return newObject;
	}

	public static UUID findId(Object entity) throws Exception {
		Field idField = StaticData.classFields.get(entity.getClass()).stream()
				.filter(a -> a.getName().equals(SqlQueryWriterServiceImpl.defaultIdColumn)).findFirst().get();
		UUID id = idField.get(entity) != null ? (UUID) idField.get(entity) : null;

		return id;
	}

	public static String[] fieldsForUpdate(Object entity) {
		List<Field> fields = StaticData.classFields.get(entity.getClass()).stream().collect(Collectors.toList());
		String[] fieldArray = fields.stream().map(a -> AppStartUpImpl.findColumnName(a)).toArray(String[]::new);
		return fieldArray;
	}

	@SuppressWarnings({ "rawtypes" })
	public static Map<Integer, Object> createParameters(Object entity) throws Exception {
		List<Field> fields = StaticData.classFields.get(entity.getClass()).stream()
				.filter(a -> !a.getName().equals(SqlQueryWriterServiceImpl.defaultIdColumn))
				.collect(Collectors.toList());
		Map<Integer, Object> parameters = new HashMap<>();
		int index = 0;
		for (Field field : fields) {
			ColumnType columnType = ExecuteNativeQueryImpl.findColumnType(field.getType());
			index++;
			if (columnType != null) {
				parameters.put(index, field.get(entity));
			} else {
				Object value = field.get(entity);
				if (value == null) {
					parameters.put(index, value);
					continue;
				}
				if (field.getType().isEnum()) {
					Enum enumValue = (Enum) value;
					parameters.put(index, enumValue.name());
				} else {
					UUID uuid = findId(value);
					parameters.put(index, uuid);
				}
			}

		}

		UUID id = findId(entity);
		if (id != null) {
			index++;
			parameters.put(index, id);
		}
		return parameters;
	}

	public static String findTableName(Object entity) {
		return entity.getClass().getAnnotation(Table.class).name();
	}

}
