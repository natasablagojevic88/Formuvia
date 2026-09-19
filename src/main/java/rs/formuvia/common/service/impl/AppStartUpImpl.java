package rs.formuvia.common.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.ws.rs.WebApplicationException;
import rs.formuvia.Formuvia;
import rs.formuvia.common.entity.Test;
import rs.formuvia.common.service.AppStartUp;
import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.CheckTables;
import rs.formuvia.database.utils.ColumnInfo;
import rs.formuvia.database.utils.ExecuteNativeQueryImpl;
import rs.formuvia.database.utils.ForeignKeyInfo;
import rs.formuvia.database.utils.IndexInfo;
import rs.formuvia.database.utils.LoadStaticData;
import rs.formuvia.database.utils.TableInfo;
import rs.formuvia.database.utils.UniqueConstraintInfo;
import rs.formuvia.utils.CheckAdmin;
import rs.formuvia.utils.InitScriptExecute;
import rs.formuvia.utils.MenuInfo;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

public class AppStartUpImpl implements AppStartUp {

	Logger logger = LogManager.getLogger(getClass());

	private static final String CUSTOM_APP_PATH = "custom.properties.path";
	private static final String APP_PATH = "application.properties";
	public static final String DRIVER_CLASS = "org.postgresql.Driver";
	public static final String CONNECTION_URL = "database.url";
	public static final String CONNECTION_USERNAME = "database.username";
	public static final String CONNECTION_PASSWORD = "database.password";
	public static final String CONNECTION_NUMBER = "database.num.connections";
	public static final String APP_NAME = "application.name";
	public static final String SET_APP_NAME = "SELECT set_config('application_name', ?, false)";
	private final String INIT_SCRIPT_EXECUTE = "execute.scripts";
	private final String MENU_FILE = "menu.xml";
	private DatabaseService databaseService = new DatabaseServiceImpl();

	@Override
	public void loadClass() {
		Reflections reflections = new Reflections(Formuvia.PACKAGE_NAME, Scanners.SubTypes.filterResultsBy(a -> true));
		StaticData.allClasses = reflections.getSubTypesOf(Object.class).stream().toList();
		for (Class<?> inClass : StaticData.allClasses) {
			List<Field> fields = new ArrayList<>();
			for (Field field : inClass.getDeclaredFields()) {
				field.setAccessible(true);
				fields.add(field);
			}
			StaticData.classFields.put(inClass, fields);
		}
	}

	@Override
	public void initParams() {

		InputStream applicationPropertiesInputStream = getClass().getClassLoader().getResourceAsStream(APP_PATH);

		try {
			StaticData.appProperties.load(applicationPropertiesInputStream);
			if (StringUtils.hasText(StaticData.appProperties.get(CUSTOM_APP_PATH).toString())) {
				File file = new File(StaticData.appProperties.get(CUSTOM_APP_PATH).toString());
				if (file.exists()) {
					StaticData.appProperties.load(new FileInputStream(file));
				}
			}

			Enumeration<Object> enumeration = StaticData.appProperties.keys();
			while (enumeration.hasMoreElements()) {
				String key = enumeration.nextElement().toString();
				if (System.getProperty(key) != null) {
					StaticData.appProperties.put(key, System.getProperty(key));
				}
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new WebApplicationException(e);
		}

	}

	@Override
	public void initConnections() {
		Integer numberOfConnections = Integer.valueOf(StaticData.appProperties.getProperty(CONNECTION_NUMBER));
		try {
			Class.forName(DRIVER_CLASS);
			for (int i = 0; i < numberOfConnections; i++) {
				Connection connection = createConnection();
				StaticData.connections.offer(connection);
			}
		} catch (Exception e) {
			this.logger.error(e.getMessage(), e);
			throw new WebApplicationException(e);
		}
	}

	@Override
	public Connection createConnection() throws Exception {
		String databaseUrl = StaticData.appProperties.getProperty(CONNECTION_URL);
		String databaseUsername = StaticData.appProperties.getProperty(CONNECTION_USERNAME);
		String databasePassword = StaticData.appProperties.getProperty(CONNECTION_PASSWORD);
		String appName = StaticData.appProperties.getProperty(APP_NAME);

		Connection connection = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword);
		connection.setAutoCommit(false);

		PreparedStatement preparedStatement = connection.prepareStatement(SET_APP_NAME);
		preparedStatement.setString(1, appName);
		preparedStatement.execute();
		preparedStatement.close();
		connection.commit();
		StaticData.allConnections.add(connection);

		return connection;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void checkTables() {

		List<TableInfo> tableInfos = new ArrayList<>();
		List<Class<?>> entityClasses = StaticData.allClasses.stream().filter(a -> a.isAnnotationPresent(Entity.class))
				.collect(Collectors.toList());

		for (Class<?> entityClass : entityClasses) {
			Table table = entityClass.getAnnotation(Table.class);
			String tableName = table.name();
			TableInfo tableInfo = new TableInfo();
			tableInfo.setName(tableName);

			List<Field> columnFields = StaticData.classFields
					.get(entityClass).stream().filter(a -> a.isAnnotationPresent(Id.class)
							|| a.isAnnotationPresent(JoinColumn.class) || a.isAnnotationPresent(Column.class))
					.collect(Collectors.toList());
			for (Field field : columnFields) {
				ColumnInfo columnInfo = new ColumnInfo();
				columnInfo.setName(findColumnName(field));
				columnInfo.setTableName(tableName);

				ColumnType columnType = ExecuteNativeQueryImpl.findColumnType(field.getType());
				if (columnType == null) {
					if (field.getType().isEnum()) {
						columnInfo.setColumnType(ColumnType.STRING);
						Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) field.getType();
						columnInfo.setListOfValues(
								Arrays.asList(enumClass.getEnumConstants()).stream().map(a -> a.name()).toList());

					} else {
						columnInfo.setColumnType(ColumnType.UUID);
					}
				} else {
					columnInfo.setColumnType(columnType);
				}

				if (field.isAnnotationPresent(Id.class)) {
					columnInfo.setIsPrimary(true);
					columnInfo.setColumnType(ColumnType.UUID);
				}

				if (field.isAnnotationPresent(Column.class)) {
					Column column = field.getAnnotation(Column.class);

					columnInfo.setNullable(column.nullable());
					columnInfo.setLength(column.length());
					columnInfo.setScale(column.scale());

					if (columnInfo.getColumnType().equals(ColumnType.BIGDECIMAL) && columnInfo.getScale() == 0)
						columnInfo.setScale(2);

					if (StringUtils.hasText(column.columnDefinition()))
						columnInfo.setColumnDefinition(column.columnDefinition());
				}

				if (field.isAnnotationPresent(JoinColumn.class)) {
					JoinColumn column = field.getAnnotation(JoinColumn.class);

					columnInfo.setNullable(column.nullable());
					if (StringUtils.hasText(column.columnDefinition()))
						columnInfo.setColumnDefinition(column.columnDefinition());

					ForeignKey foreignKey = column.foreignKey();
					ForeignKeyInfo foreignKeyInfo = new ForeignKeyInfo();
					foreignKeyInfo.setName(foreignKey.name());
					foreignKeyInfo.setTableName(tableName);
					foreignKeyInfo.setReferenceTable(field.getType().getAnnotation(Table.class).name());
					foreignKeyInfo.setColumnName(columnInfo.getName());

					if (field.isAnnotationPresent(ManyToOne.class)) {
						ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
						if (manyToOne.cascade().length > 0) {
							if (manyToOne.cascade()[0].equals(CascadeType.REMOVE)) {
								foreignKeyInfo.setCascadeDelete(true);
							}
						}
					}

					tableInfo.getForeignKeys().add(foreignKeyInfo);
				}

				tableInfo.getColumns().add(columnInfo);
			}

			for (UniqueConstraint uniqueConstraint : table.uniqueConstraints()) {
				UniqueConstraintInfo uniqueConstraintInfo = new UniqueConstraintInfo();
				uniqueConstraintInfo.setColumns(uniqueConstraint.columnNames());
				uniqueConstraintInfo.setTableName(tableName);
				uniqueConstraintInfo.setName(uniqueConstraint.name());
				tableInfo.getUniqueContraints().add(uniqueConstraintInfo);
			}

			for (Index index : table.indexes()) {
				IndexInfo indexInfo = new IndexInfo();
				indexInfo.setColumnName(index.columnList());
				indexInfo.setTableName(tableName);
				indexInfo.setName(index.name());
				tableInfo.getIndexes().add(indexInfo);
			}

			tableInfos.add(tableInfo);
		}

		this.databaseService.executeQuery(new CheckTables(tableInfos));

	}

	public static String findColumnName(Field field) {
		String columnName = field.getName();

		if (field.isAnnotationPresent(Column.class))
			if (StringUtils.hasText(field.getAnnotation(Column.class).name()))
				columnName = field.getAnnotation(Column.class).name();

		if (field.isAnnotationPresent(JoinColumn.class))
			if (StringUtils.hasText(field.getAnnotation(JoinColumn.class).name()))
				columnName = field.getAnnotation(JoinColumn.class).name();

		return columnName;
	}

	@Override
	public void checkAdminUser() {
		CheckAdmin checkAdmin = new CheckAdmin();
		this.databaseService.executeQuery(checkAdmin);
	}

	@Override
	public void initStaticData() {
		LoadStaticData loadStaticData = new LoadStaticData();
		this.databaseService.executeQuery(loadStaticData);
	}

	@Override
	public void initScriptsExecute() {
		if (!Boolean.valueOf(StaticData.appProperties.getProperty(INIT_SCRIPT_EXECUTE))) {
			return;
		}

		Test test = new Test();
		test.setKratakTekst(UUID.randomUUID().toString());
		this.databaseService.save(test);

		InitScriptExecute initScriptExecute = new InitScriptExecute();
		this.databaseService.executeQuery(initScriptExecute);
	}

	@Override
	public void loadMenu() {
		SAXBuilder saxBuilder = new SAXBuilder();
		Document document = null;
		try {
			document = saxBuilder.build(this.getClass().getClassLoader().getResourceAsStream(MENU_FILE));
		} catch (Exception e) {
			this.logger.error(e.getMessage(), e);
		}

		Element root = document.getRootElement();

		for (Element menu : root.getChildren()) {
			loadMenuChild(menu, null);
		}

	}

	private MenuInfo loadMenuChild(Element menu, MenuInfo parent) {
		MenuInfo menuInfo = new MenuInfo();
		menuInfo.setIcon(menu.getAttributeValue("icon"));
		menuInfo.setName(menu.getAttributeValue("name"));
		menuInfo.setRole(menu.getAttributeValue("role"));
		menuInfo.setUrl(menu.getAttributeValue("url"));
		
		for(Element child:menu.getChildren()) {
			loadMenuChild(child, menuInfo);
		}

		if (parent==null)
			StaticData.menuInfos.add(menuInfo);
		else
			parent.getItems().add(menuInfo);

		return menuInfo;

	}

}
