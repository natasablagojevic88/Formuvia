package rs.formuvia.common.utils;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import rs.formuvia.exceptions.ForbiddenException;
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
	private DatabaseService databaseService = new DatabaseServiceImpl();

	@Override
	public List<HistoryDTO> execute(Connection connection) throws Exception {
		List<HistoryDTO> list = new ArrayList<>();
		Class<?> dtoClass = StaticData.allClassesByName.get(className);
		String tableName = GenerateQueryFromDTO.findTableName(dtoClass);
		Class<?> tableClass = dtoClass.getAnnotation(EntityClass.class).value();

		String[] roles = dtoClass.getAnnotation(EntityClass.class).roles();

		boolean hasRole = false;
		for (String role : roles) {
			if (commonService.getRoles().contains(role)) {
				hasRole = true;
				break;
			}
		}
		if (!hasRole) {
			throw new ForbiddenException();
		}
		DatabaseParameter databaseParameter = new DatabaseParameter();
		databaseParameter.getFilters().add(DatabaseFilter.valueOf("tableName", tableName));
		databaseParameter.getFilters().add(DatabaseFilter.valueOf("dataId", id.toString()));
		databaseParameter.getOrders().add(QueryDatabaseOrder.valueOf("dateTime", Direction.DESC));

		List<TrackDTO> tracks = this.databaseService.findAll(databaseParameter, TrackDTO.class, connection);
		List<Field> fields = StaticData.classFields.get(dtoClass).stream()
				.filter(a -> !a.isAnnotationPresent(SkipColumn.class)).collect(Collectors.toList());

		for (TrackDTO track : tracks) {
			HistoryDTO historyDTO = new HistoryDTO();
			historyDTO.setAction(resourceBundleService.getText("TrackAction." + track.getAction().name()));
			historyDTO.setAppUserName(track.getAppUserName());
			historyDTO.setAppUserSurname(track.getAppUserSurname());
			historyDTO.setAppUserUsername(track.getAppUserUsername());
			historyDTO.setTime(track.getDateTime());

			Object oldData = createObjectFromJson(tableClass, track.getOldData(), connection);
			Object newData = createObjectFromJson(tableClass, track.getNewData(), connection);

			Object oldDataDTO = modelMapper.map(oldData, dtoClass);
			Object newDataDTO = modelMapper.map(newData, dtoClass);

			for (Field field : fields) {
				ChangeDTO changeDTO = createChangeDTO(field, field.get(oldDataDTO), field.get(newDataDTO));

				if (StringUtils.notNull(changeDTO))
					historyDTO.getChanges().add(changeDTO);
			}

			list.add(historyDTO);
		}
		return list;
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

		JsonReader jsonReader = Json.createReader(new StringReader(jsonText));
		JsonObject jsonObject = jsonReader.readObject();

		for (Field field : fields) {
			String columnName = AppStartUpImpl.findColumnName(field);

			if (StringUtils.isNull(jsonObject.get(columnName))) {
				continue;
			}

			Object value = null;
			switch (jsonObject.get(columnName).getValueType()) {
			case NULL:
				continue;

			case STRING:
				value = ((JsonString) jsonObject.get(columnName)).getString();
				break;
			default: {
				value = jsonObject.get(columnName).toString();
			}

			}

			ColumnType columnType = ExecuteNativeQueryImpl.findColumnType(field.getType());

			if (columnType != null) {
				try {
					switch (columnType) {
					case BIGDECIMAL:
						BigDecimal bigDecimalValue = new BigDecimal(value.toString());
						field.set(object, bigDecimalValue);
						break;
					case BOOLEAN:
						field.set(object, Boolean.valueOf(value.toString()));
						break;
					case INTEGER:
						field.set(object, Integer.valueOf(value.toString()));
						break;
					case LOCALDATE:
						field.set(object, LocalDate.parse(value.toString()));
						break;
					case LOCALDATETIME:
						field.set(object, LocalDateTime.parse(value.toString()));
						break;
					case LONG:
						field.set(object, Long.valueOf(value.toString()));
						break;
					case STRING:
						field.set(object, value.toString());
						break;
					case UUID:
						field.set(object, UUID.fromString(value.toString()));
						break;
					}
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
						field.set(object, this.databaseService.findById(UUID.fromString(value.toString()),
								field.getType(), connection));
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

	private ChangeDTO createChangeDTO(Field field, Object oldData, Object newData) {
		if (StringUtils.isNull(oldData) && StringUtils.isNull(newData)) {
			return null;
		}

		if (field.getDeclaringClass().equals(AppUserDTO.class) && field.getName().equals("password")) {
			return null;
		}

		ChangeDTO changeDTO = new ChangeDTO();
		changeDTO.setFieldName(this.resourceBundleService.getText(CustomDefaultExceptionMapper.createFieldName(field)));
		ColumnType columnType = ExecuteNativeQueryImpl.findColumnType(field.getType());
		changeDTO.setColumnType(columnType == null ? ColumnType.STRING : columnType);
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
