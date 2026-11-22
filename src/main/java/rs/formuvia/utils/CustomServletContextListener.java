package rs.formuvia.utils;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.websocket.CloseReason;
import jakarta.websocket.CloseReason.CloseCodes;
import jakarta.websocket.Session;
import rs.formuvia.common.service.AppStartUp;
import rs.formuvia.common.service.impl.AppStartUpImpl;
import rs.formuvia.quartz.service.QuartzService;
import rs.formuvia.quartz.service.impl.QuartzServiceImpl;

@WebListener
public class CustomServletContextListener implements ServletContextListener {

	public static String context;
	private Logger logger = LogManager.getLogger(getClass());
	private QuartzService quartzService = new QuartzServiceImpl();
	private final String WEBSOCKET_SESSION_SHUTDOWN = "Server shutting down";

	@Override
	public void contextInitialized(ServletContextEvent sce) {

		context = sce.getServletContext().getContextPath();

		AppStartUp appStartUp = new AppStartUpImpl();
		appStartUp.loadClass();
		appStartUp.initParams();
		PostgresAppender.checkConnection();
		appStartUp.initConnections();
		appStartUp.checkTables();
		appStartUp.checkAdminUser();
		appStartUp.initStaticData();
		appStartUp.initScriptsExecute();
		appStartUp.loadMenu();
		appStartUp.loadCustomResourceBudle();

		quartzService.initScheduler();
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		StaticData.shutingDown = true;
		quartzService.closeScheduler();
		LogManager.shutdown();
		for (Connection connection : new ArrayList<>(StaticData.allConnections)) {
			try {
				connection.close();
			} catch (Exception e) {
				this.logger.error(e.getMessage(), e);
			}
		}

		try {
			StaticData.postgresAppenderConnection.close();
		} catch (Exception e) {
			this.logger.error(e.getMessage(), e);
		}

		try {
			StaticData.databaseListenConnection.close();
		} catch (Exception e) {
			this.logger.error(e.getMessage(), e);
		}

		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Enumeration<Driver> drivers = DriverManager.getDrivers();
		while (drivers.hasMoreElements()) {
			Driver driver = drivers.nextElement();
			if (driver.getClass().getClassLoader() == cl) {
				try {
					DriverManager.deregisterDriver(driver);
				} catch (SQLException ignore) {
				}
			}
		}

		Iterator<UUID> itrSession = StaticData.websocketSession.keySet().iterator();
		while (itrSession.hasNext()) {
			UUID userid = itrSession.next();
			for (Session session : StaticData.websocketSession.get(userid))
				try {
					session.close(new CloseReason(CloseCodes.GOING_AWAY, WEBSOCKET_SESSION_SHUTDOWN));
				} catch (Exception e) {
					this.logger.error(e.getMessage(), e);
				}
		}

	}
}
