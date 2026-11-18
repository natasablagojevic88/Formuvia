package rs.formuvia.utils;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.impl.AppStartUpImpl;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.quartz.jobs.DatabaseListenCheckConnectionJob;

@Plugin(name = "Postgres", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class PostgresAppender extends AbstractAppender {

	protected PostgresAppender(String name, Filter filter, Layout<? extends Serializable> layout,
			boolean ignoreExceptions, Property[] properties) {
		super(name, filter, layout, ignoreExceptions, properties);

	}

	private static Connection connection;
	private static final String SUFIX_FOR_DATABASE_NAME = "-logger";
	private static String query;
	private static CommonService commonService = new CommonServiceImpl();
	private static final String QUERY_FILE = "insert_to_log.sql";

	public static void checkConnection() {

		try {
			PreparedStatement preparedStatement = connection
					.prepareStatement(StaticData.appProperties.getProperty(DatabaseServiceImpl.CHECK_QUERY));
			preparedStatement.execute();
			preparedStatement.close();
		} catch (Exception e) {
			try {
				Class.forName(AppStartUpImpl.DRIVER_CLASS);
				query = commonService.readQueryFromFile(QUERY_FILE);
				connection = DriverManager.getConnection(
						StaticData.appProperties.getProperty(AppStartUpImpl.CONNECTION_URL),
						StaticData.appProperties.getProperty(AppStartUpImpl.CONNECTION_USERNAME),
						StaticData.appProperties.getProperty(AppStartUpImpl.CONNECTION_PASSWORD));
				DatabaseListenCheckConnectionJob.setAppName(connection, SUFIX_FOR_DATABASE_NAME);
			} catch (Exception e1) {
			}
		}
	}

	@PluginFactory
	public static PostgresAppender create(@PluginAttribute("name") String name, @PluginElement("Filter") Filter filter,
			@PluginAttribute(value = "ignoreExceptions", defaultBoolean = true) boolean ignoreExceptions,
			@PluginAttribute(value = "batchSize", defaultInt = 100) int batchSize) {
		return new PostgresAppender(name, filter, null, ignoreExceptions, Property.EMPTY_ARRAY);
	}

	@Override
	public void append(LogEvent event) {
		checkConnection();
		try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
			preparedStatement.setObject(1, LocalDateTime.now());
			preparedStatement.setObject(2, event.getLevel() == null ? null : event.getLevel().name());
			preparedStatement.setObject(3, event.getLoggerName());
			preparedStatement.setObject(4, event.getMessage() == null ? null : event.getMessage().toString());
			preparedStatement.setObject(5, event.getThrown() == null ? null : stackTrace(event.getThrown()));
			preparedStatement.execute();
			preparedStatement.close();
		} catch (Exception ex) {
		}

	}

	private static String stackTrace(Throwable thrown) {
		if (thrown == null) {
			return null;
		}
		StringWriter writer = new StringWriter();
		thrown.printStackTrace(new PrintWriter(writer));
		return writer.toString();
	}

}
