package rs.formuvia.quartz.jobs;

import java.util.Arrays;

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
import rs.formuvia.database.utils.LoadStaticData;
import rs.formuvia.model.dto.ModelColumnDTO;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.utils.UpdateColumnModel;
import rs.formuvia.utils.DatabaseListen;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

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
				if (Arrays.asList(DatabaseListen.values()).stream().map(a -> a.name()).toList()
						.contains(nameNotification)) {
					DatabaseListen databaseListen = DatabaseListen.valueOf(nameNotification);
					switch (databaseListen) {
					case listen_appuser, listen_appuser_role:
						StaticData.appUsers = this.databaseService.findAll(null, AppUser.class);
						StaticData.appUserRoles = this.databaseService.findAll(null, AppUserRoleDTO.class);
						break;
					case listen_role:
						StaticData.roles = this.databaseService.findAll(null, RoleDTO.class);
						break;
					case listen_model:
						StaticData.models = this.databaseService.findAll(null, ModelDTO.class);
						break;
					case listen_model_column:
						StaticData.modelColumns = this.databaseService.findAll(null, ModelColumnDTO.class);
						break;

					}
				} else {
					String modelCode = nameNotification.substring(UpdateColumnModel.LISTEN_PREFIX.length());
					ModelDTO modelDTO = StaticData.models.stream().filter(a -> StringUtils.notNull(a.getCode()))
							.filter(a -> a.getCode().equals(modelCode)).findFirst().orElse(null);
					if (StringUtils.notNull(modelDTO)) {
						LoadStaticData.loadStaticDataCodebookModel(modelDTO.getId(), null);
					}
				}

				;
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}

}
