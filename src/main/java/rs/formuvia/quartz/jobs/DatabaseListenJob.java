package rs.formuvia.quartz.jobs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.postgresql.PGNotification;
import org.postgresql.jdbc.PgConnection;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import rs.formuvia.administration.dto.AppUserRoleDTO;
import rs.formuvia.administration.dto.RoleDTO;
import rs.formuvia.administration.entity.AppUser;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.utils.DatabaseListen;
import rs.formuvia.utils.StaticData;

@DisallowConcurrentExecution
public class DatabaseListenJob implements Job {
	private Logger logger = LogManager.getLogger(getClass());
	private DatabaseService databaseService = new DatabaseServiceImpl();

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		try {
			if (StaticData.databaseListenConnection.isClosed()) {
				return;
			}
			PGNotification[] pgNotifications = StaticData.databaseListenConnection.unwrap(PgConnection.class)
					.getNotifications();
			for (PGNotification notification : pgNotifications) {
				String nameNotification = notification.getName();
				DatabaseListen databaseListen = DatabaseListen.valueOf(nameNotification);
				switch (databaseListen) {
				case appuser_listen, appuser_role_listen:
					StaticData.appUsers = this.databaseService.findAll(null, AppUser.class);
					StaticData.appUserRoles = this.databaseService.findAll(null, AppUserRoleDTO.class);
					break;
				case role_listen:
					StaticData.roles = this.databaseService.findAll(null, RoleDTO.class);
					break;
				}

				;
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}

}
