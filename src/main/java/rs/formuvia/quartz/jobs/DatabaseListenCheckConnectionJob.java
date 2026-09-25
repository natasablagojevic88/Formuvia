package rs.formuvia.quartz.jobs;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import rs.formuvia.common.service.impl.AppStartUpImpl;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.LoadStaticData;
import rs.formuvia.model.utils.UpdateColumnModel;
import rs.formuvia.utils.DatabaseListen;
import rs.formuvia.utils.StaticData;

public class DatabaseListenCheckConnectionJob implements Job {
	private static final String SUFIX_FOR_DATABASE_LISTEN = "-database.listen";
	public static final String LISTEN_QUERY = "LISTEN ";

	public static void checkConnection() {
		String databaseUrl = StaticData.appProperties.getProperty(AppStartUpImpl.CONNECTION_URL);
		String databaseUsername = StaticData.appProperties.getProperty(AppStartUpImpl.CONNECTION_USERNAME);
		String databasePassword = StaticData.appProperties.getProperty(AppStartUpImpl.CONNECTION_PASSWORD);
		String checkQuery = StaticData.appProperties.getProperty(DatabaseServiceImpl.CHECK_QUERY);
		String appName = StaticData.appProperties.getProperty(AppStartUpImpl.APP_NAME);

		try {
			PreparedStatement preparedStatement = StaticData.databaseListenConnection.prepareStatement(checkQuery);
			ResultSet rs = preparedStatement.executeQuery();
			rs.close();
			preparedStatement.close();
		} catch (Exception e) {
			try {
				StaticData.databaseListenConnection = DriverManager.getConnection(databaseUrl, databaseUsername,
						databasePassword);
				String queryForApplicationName = AppStartUpImpl.SET_APP_NAME;
				PreparedStatement preparedStatement = StaticData.databaseListenConnection
						.prepareStatement(queryForApplicationName);
				preparedStatement.setObject(1, appName + SUFIX_FOR_DATABASE_LISTEN);
				preparedStatement.execute();
				preparedStatement.close();

				Statement statement = StaticData.databaseListenConnection.createStatement();
				for (DatabaseListen databaseListen : DatabaseListen.values()) {
					statement.execute(LISTEN_QUERY + databaseListen.name());
				}
				statement.close();

				Set<UUID> columnsWithCodebook = LoadStaticData.columnWithCodebook();

				for (UUID modelId : columnsWithCodebook) {
					UpdateColumnModel.initListen(modelId);
				}
			} catch (Exception e2) {
				Logger logger = LogManager.getLogger(DatabaseListenJob.class);
				logger.error(e2.getMessage(), e2);
			}
		}
	}

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		checkConnection();

	}

}
