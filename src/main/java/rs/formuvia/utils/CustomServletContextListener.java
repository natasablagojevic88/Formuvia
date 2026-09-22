package rs.formuvia.utils;

import java.sql.Connection;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import rs.formuvia.common.service.AppStartUp;
import rs.formuvia.common.service.impl.AppStartUpImpl;
import rs.formuvia.quartz.service.QuartzService;
import rs.formuvia.quartz.service.impl.QuartzServiceImpl;

@WebListener
public class CustomServletContextListener implements ServletContextListener {

	public static String context;
	private Logger logger = LogManager.getLogger(getClass());
	private QuartzService quartzService = new QuartzServiceImpl();

	@Override
	public void contextInitialized(ServletContextEvent sce) {

		context = sce.getServletContext().getContextPath();

		AppStartUp appStartUp = new AppStartUpImpl();
		appStartUp.loadClass();
		appStartUp.initParams();
		appStartUp.initConnections();
		appStartUp.checkTables();
		appStartUp.checkAdminUser();
		appStartUp.initStaticData();
		appStartUp.initScriptsExecute();
		appStartUp.loadMenu();

		quartzService.initScheduler();
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		for (Connection connection : new ArrayList<>(StaticData.allConnections)) {
			try {
				connection.close();
			} catch (Exception e) {
				this.logger.error(e.getMessage(), e);
			}
		}

		quartzService.closeScheduler();
	}
}
