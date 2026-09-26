package rs.formuvia.common.utils;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import rs.formuvia.administration.dto.AppUserDTO;
import rs.formuvia.common.dto.ChangeDTO;
import rs.formuvia.common.dto.HistoryDTO;
import rs.formuvia.common.dto.TrackDTO;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.ResourceBundleService;
import rs.formuvia.common.service.impl.AppStartUpImpl;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.common.service.impl.ResourceBundleServiceImpl;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.database.annotations.SkipColumn;
import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.database.enums.Direction;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.ExecuteNativeQueryImpl;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.database.utils.GenerateQueryFromDTO;
import rs.formuvia.database.utils.QueryDatabaseOrder;
import rs.formuvia.utils.CustomDefaultExceptionMapper;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

public class CreateHistory implements ExecuteQuery<List<HistoryDTO>> {

	private final HttpServletRequest httpServletRequest;
	private final String className;
	private final UUID id;
	private final ResourceBundleService resourceBundleService;
	private final CommonService commonService;

	public CreateHistory(HttpServletRequest httpServletRequest, String className, UUID id) {
		this.httpServletRequest = httpServletRequest;
		this.className = className;
		this.id = id;
		this.resourceBundleService = new ResourceBundleServiceImpl(this.httpServletRequest);
		this.commonService = new CommonServiceImpl(this.httpServletRequest);

	}

	private ModelMapper modelMapper = new ModelMapper();
	private static DatabaseService databaseService = new DatabaseServiceImpl();

	@Override
	public List<HistoryDTO> execute(Connection connection) throws Exception {
		List<HistoryDTO> list = new ArrayList<>();
		Class<?> dtoClass = StaticData.allClassesByName.get(className);
		String tableName = GenerateQueryFromDTO.findTableName(dtoClass);
		Class<?> tableClass = dtoClass.getAnnotation(EntityClass.class).value();

		String[] roles = dtoClass.getAnnotation(EntityClass.class).roles();

		commonService.checkRole(roles);

		List<TrackDTO> tracks = createListTrackDTOs(tableName, id, connection);
		List<Field> fields = StaticData.classFields.get(dtoClass).stream()
				.filter(a -> !a.isAnnotationPresent(SkipColumn.class)).collect(Collectors.toList());

		for (TrackDTO track : tracks) {
			HistoryDTO historyDTO = createHistoryDTO(track, resourceBundleService);

			Object oldData = createObjectFromJson(tableClass, track.getOldData(), connection);
			Object newData = createObjectFromJson(tableClass, track.getNewData(), connection);

			Object oldDataDTO = modelMapper.map(oldData, dtoClass);
			Object newDataDTO = modelMapper.map(newData, dtoClass);

			for (Field field : fields) {

				if (field.getDeclaringClass().equals(AppUserDTO.class) && field.getName().equals("password")) {
					continue;
				}
				String fieldName = CustomDefaultExceptionMapper.createFieldName(field);
				ColumnType columnType = ExecuteNativeQueryImpl.findColumnType(field.getType());
				columnType = columnType == null ? ColumnType.STRING : columnType;
				ChangeDTO changeDTO = createChangeDTO(field.get(oldDataDTO), field.get(newDataDTO),
						this.resourceBundleService, fieldName, columnType);

				if (StringUtils.notNull(changeDTO))
					historyDTO.getChanges().add(changeDTO);
			}

			list.add(historyDTO);
		}
		return list;
	}

	public static HistoryDTO createHistoryDTO(TrackDTO track, ResourceBundleService resourceBundleService) {
		HistoryDTO historyDTO = new HistoryDTO();
		historyDTO.setAction(resourceBundleService.getText("TrackAction." + track.getAction().name()));
		historyDTO.setAppUserName(track.getAppUserName());
		historyDTO.setAppUserSurname(track.getAppUserSurname());
		historyDTO.setAppUserUsername(track.getAppUserUsername());
		historyDTO.setTime(track.getDateTime());
		return historyDTO;
	}

	public static List<TrackDTO> createListTrackDTOs(String tableName, UUID id, Connection connection) {
		DatabaseParameter databaseParameter = new DatabaseParameter();
		databaseParameter.getFilters().add(DatabaseFilter.valueOf("tableName", tableName));
		databaseParameter.getFilters().add(DatabaseFilter.valueOf("dataId", id.toString()));
		databaseParameter.getOrders().add(QueryDatabaseOrder.valueOf("dateTime", Direction.DESC));

		List<TrackDTO> tracks = databaseService.findAll(databaseParameter, TrackDTO.class, connection);
		return tracks;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Object createObjectFromJson(Class<?> inClass, String jsonText, Connection connection) {
		List<Field> fields = StaticData.classFields.get(inClass).stream().collect(Collectors.toList());
		Object object = null;
		try {
			object = inClass.getConstructor().newInstance();
		} catch (Exception e) {
			return new WebApplicationException(e);
		}

		if (!StringUtils.hasText(jsonText)) {
			return object;
		}

		JsonObject jsonObject = createJsonObject(jsonText);
		for (Field field : fields) {
			String columnName = AppStartUpImpl.findColumnName(field);

			if (StringUtils.isNull(jsonObject.get(columnName))) {
				continue;
			}

			Object value = getObjectFromJson(jsonObject, columnName);
			if (StringUtils.isNull(value)) {
				continue;
			}

			ColumnType columnType = ExecuteNativeQueryImpl.findColumnType(field.getType());

			if (columnType != null) {
				try {
					field.set(object, convertObjectByType(columnType, value));
				} catch (Exception e) {
					throw new WebApplicationException(e);
				}
			} else {

				if (field.getType().isEnum()) {
					try {
						Class<? extends Enum> enumClass = (Class<? extends Enum>) field.getType();
						field.set(object, Enum.valueOf(enumClass, value.toString()));
					} catch (Exception e) {
						throw new WebApplicationException(e);
					}
				} else {
					try {
						field.set(object, databaseService.findById(UUID.fromString(value.toString()), field.getType(),
								connection));
					} catch (Exception e) {
						try {
							field.set(object, null);
						} catch (Exception e1) {
							throw new WebApplicationException(e1);
						}
					}
				}

			}
		}

		return object;
	}

	public static Object getObjectFromJson(JsonObject jsonObject, String fieldName) {
		Object value = null;
		switch (jsonObject.get(fieldName).getValueType()) {
		case NULL:
			return null;

		case STRING:
			value = ((JsonString) jsonObject.get(fieldName)).getString();
			return value;
		default: {
			value = jsonObject.get(fieldName).toString();
			return value;
		}

		}
	}

	public static Object convertObjectByType(ColumnType columnType, Object value) {
		switch (columnType) {
		case BIGDECIMAL:
			BigDecimal bigDecimalValue = new BigDecimal(value.toString());
			return bigDecimalValue;
		case BOOLEAN:
			return Boolean.valueOf(value.toString());
		case INTEGER:
			return Integer.valueOf(value.toString());
		case LOCALDATE:
			return LocalDate.parse(value.toString());
		case LOCALDATETIME:
			return LocalDateTime.parse(value.toString());
		case LOCALTIME:
			return LocalTime.parse(value.toString());
		case LONG:
			return Long.valueOf(value.toString());
		case STRING:
			return value.toString();
		case UUID:
			return UUID.fromString(value.toString());
		}
		return null;
	}

	public static JsonObject createJsonObject(String jsonText) {
		JsonReader jsonReader = Json.createReader(new StringReader(jsonText));
		return jsonReader.readObject();
	}

	public static ChangeDTO createChangeDTO(Object oldData, Object newData, ResourceBundleService resourceBundleService,
			String fieldName, ColumnType columnType) {
		if (StringUtils.isNull(oldData) && StringUtils.isNull(newData)) {
			return null;
		}

		ChangeDTO changeDTO = new ChangeDTO();
		changeDTO.setFieldName(resourceBundleService.getText(fieldName));
		changeDTO.setColumnType(columnType);
		if (StringUtils.isNull(oldData) && StringUtils.notNull(newData)) {
			changeDTO.setNewData(newData);
			return changeDTO;
		}

		if (StringUtils.notNull(oldData) && StringUtils.isNull(newData)) {

			changeDTO.setOldData(oldData);
			return changeDTO;
		}

		if (!oldData.equals(newData)) {
			changeDTO.setOldData(oldData);
			changeDTO.setNewData(newData);
			return changeDTO;
		}

		return null;
	}
}
