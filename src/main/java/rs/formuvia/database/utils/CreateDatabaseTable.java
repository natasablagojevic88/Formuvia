package rs.formuvia.database.utils;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import rs.formuvia.common.dto.ComboboxDTO;
import rs.formuvia.common.service.ResourceBundleService;
import rs.formuvia.common.service.impl.ResourceBundleServiceImpl;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.database.annotations.HideInTable;
import rs.formuvia.database.annotations.NotEditableInTable;
import rs.formuvia.database.annotations.SkipColumn;
import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.SqlQueryWriterService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.service.impl.SqlQueryWriterServiceImpl;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

public class CreateDatabaseTable<C> implements ExecuteQuery<DatabaseTable<C>> {

	private final DatabaseParameter databaseParameter;
	private final Class<C> resultClass;
	private final HttpServletRequest httpServletRequest;

	private final String SUFIX_TITLE_NAME = ".title";

	private final DatabaseService databaseService = new DatabaseServiceImpl();
	private final SqlQueryWriterService sqlQueryWriterService = new SqlQueryWriterServiceImpl();
	private final ResourceBundleService resourceBundleService;

	public CreateDatabaseTable(DatabaseParameter databaseParameter, Class<C> resultClass,
			HttpServletRequest httpServletRequest) {
		this.databaseParameter = databaseParameter;
		this.resultClass = resultClass;
		this.httpServletRequest = httpServletRequest;
		resourceBundleService = new ResourceBundleServiceImpl(this.httpServletRequest);
	}

	@Override
	public DatabaseTable<C> execute(Connection connection) throws Exception {

		List<C> list = this.databaseService.findAll(databaseParameter, resultClass, connection);
		DatabaseTable<C> databaseTable = new DatabaseTable<>();
		databaseTable.setName(this.resourceBundleService.getText(resultClass.getSimpleName() + SUFIX_TITLE_NAME));
		databaseTable.setClassName(resultClass.getSimpleName());
		if(StringUtils.hasText(resultClass.getAnnotation(EntityClass.class).saveUrl()))
			databaseTable.setSaveUrl(resultClass.getAnnotation(EntityClass.class).saveUrl());
		addColumn(databaseTable);
		databaseTable.setList(list);
		QueryTableInfo queryTableInfo = GenerateQueryFromDTO.createQueryTableInfo(this.resultClass);
		String totalQuery = sqlQueryWriterService.createTotalQuery(queryTableInfo, databaseParameter);
		Map<Integer, Object> parameters = sqlQueryWriterService.createParameters(databaseParameter.getFilters());
		Long total = this.databaseService.executeNativeQuery(totalQuery, parameters, Long.class, connection).getFirst();
		databaseTable.setTotal(total);
		if (total == 0) {
			databaseTable.setNumberOfPages(0);
		} else {
			Long numberOfPages = total / this.databaseParameter.getPageSize();

			if (total % this.databaseParameter.getPageSize() != 0)
				numberOfPages = numberOfPages + 1;

			databaseTable.setNumberOfPages(numberOfPages.intValue());
		}
		return databaseTable;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void addColumn(DatabaseTable<C> databaseTable) {
		List<Field> fields = StaticData.classFields.get(this.resultClass).stream()
				.collect(Collectors.toList());

		for (Field field : fields) {
			DatabaseColumn databaseColumn = new DatabaseColumn();
			databaseColumn.setFieldName(field.getName());
			databaseColumn.setDescription(
					this.resourceBundleService.getText(this.resultClass.getSimpleName() + "." + field.getName()));
			ColumnType columnType = ExecuteNativeQueryImpl.findColumnType(field.getType());
			if (columnType == null) {
				columnType = ColumnType.STRING;

				if (field.getType().isEnum()) {
					Class<? extends Enum> enumClass = (Class<? extends Enum>) field.getType();

					for (Enum enumValue : enumClass.getEnumConstants()) {
						databaseColumn.getListOfValues()
								.add(new ComboboxDTO(enumValue.name(), this.resourceBundleService
										.getText(enumClass.getSimpleName() + "." + enumValue.name())));
					}

				}
			}

			databaseColumn.setColumnType(columnType);
			
			if(field.isAnnotationPresent(NotEditableInTable.class)) {
				databaseColumn.setEditable(false);
			}
			
			if(field.isAnnotationPresent(NotNull.class)) {
				databaseColumn.setRequired(true);
			}
			
			if(!field.isAnnotationPresent(HideInTable.class)) {
				databaseTable.getColumn().add(databaseColumn);
			}
			if(!field.isAnnotationPresent(SkipColumn.class)) {
				databaseTable.getAllColumns().add(databaseColumn);
			}

		}
	}
}
