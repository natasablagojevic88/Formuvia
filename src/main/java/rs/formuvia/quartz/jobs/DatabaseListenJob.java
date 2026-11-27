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

import jakarta.websocket.Session;
import rs.formuvia.administration.dto.AppUserRoleDTO;
import rs.formuvia.administration.dto.RoleDTO;
import rs.formuvia.administration.dto.SmtpDTO;
import rs.formuvia.administration.entity.AppUser;
import rs.formuvia.bpm.dto.BpmGroupDTO;
import rs.formuvia.common.utils.NotificationListen;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.LoadStaticData;
import rs.formuvia.model.dto.ModelColumnConditionDTO;
import rs.formuvia.model.dto.ModelColumnDTO;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.dto.ModelFileDTO;
import rs.formuvia.model.dto.ModelJasperDTO;
import rs.formuvia.model.dto.ModelProcedureDTO;
import rs.formuvia.model.dto.ModelTriggerDTO;
import rs.formuvia.model.utils.UpdateColumnModel;
import rs.formuvia.quartz.dto.JobItemDTO;
import rs.formuvia.quartz.dto.JobItemWebhookDTO;
import rs.formuvia.report.dto.ReportDTO;
import rs.formuvia.report.dto.ReportGroupDTO;
import rs.formuvia.report.dto.ReportParameterDTO;
import rs.formuvia.utils.DatabaseListen;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;
import rs.formuvia.websocket.NotificationWebSocket;

@DisallowConcurrentExecution
public class DatabaseListenJob implements Job {
	private Logger logger = LogManager.getLogger(getClass());
	private DatabaseService databaseService = new DatabaseServiceImpl();
	public static final String NOTIFICATION_LISTEN = "listen_notification";

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
				try {
					if (Arrays.asList(DatabaseListen.values()).stream().map(a -> a.name()).toList()
							.contains(nameNotification)) {
						DatabaseListen databaseListen = DatabaseListen.valueOf(nameNotification);
						switch (databaseListen) {
						case listen_appuser, listen_appuser_role:
							StaticData.appUsers = this.databaseService.findAll(null, AppUser.class);
							StaticData.appUserRoles = this.databaseService.findAll(null, AppUserRoleDTO.class);
							break;
						case listen_role:
							StaticData.appUserRoles = this.databaseService.findAll(null, AppUserRoleDTO.class);
							StaticData.roles = this.databaseService.findAll(null, RoleDTO.class);
							StaticData.models = this.databaseService.findAll(null, ModelDTO.class);
							StaticData.reportGroups = this.databaseService.findAll(null, ReportGroupDTO.class);
							break;
						case listen_model:
							StaticData.models = this.databaseService.findAll(null, ModelDTO.class);
							break;
						case listen_model_column:
							StaticData.modelColumns = this.databaseService.findAll(null, ModelColumnDTO.class);
							StaticData.modelColumnsConditions = this.databaseService.findAll(null,
									ModelColumnConditionDTO.class);
							break;
						case listen_model_file:
							StaticData.modelFiles = this.databaseService.findAll(null, ModelFileDTO.class);
							StaticData.modelJaspers = this.databaseService.findAll(null, ModelJasperDTO.class);
							StaticData.reports = this.databaseService.findAll(null, ReportDTO.class);
							break;
						case listen_model_column_condition:
							StaticData.modelColumnsConditions = this.databaseService.findAll(null,
									ModelColumnConditionDTO.class);
							break;
						case listen_model_procedure:
							StaticData.modelProcedures = this.databaseService.findAll(null, ModelProcedureDTO.class);
							break;
						case listen_model_trigger:
							StaticData.modelTriggers = this.databaseService.findAll(null, ModelTriggerDTO.class);
							break;
						case listen_model_jasper:
							StaticData.modelJaspers = this.databaseService.findAll(null, ModelJasperDTO.class);
							break;
						case listen_report:
							StaticData.reports = this.databaseService.findAll(null, ReportDTO.class);
							break;
						case listen_report_group:
							StaticData.reportGroups = this.databaseService.findAll(null, ReportGroupDTO.class);
							break;
						case listen_report_parameter:
							StaticData.reportParameters = this.databaseService.findAll(null, ReportParameterDTO.class);
							break;
						case listen_job_item:
							StaticData.jobItems = this.databaseService.findAll(null, JobItemDTO.class);
							StaticData.webhooks = this.databaseService.findAll(null, JobItemWebhookDTO.class);
							break;
						case listen_smtp:
							StaticData.smtps = this.databaseService.findAll(null, SmtpDTO.class);
							break;
						case listen_job_item_webhook:
							StaticData.webhooks = this.databaseService.findAll(null, JobItemWebhookDTO.class);
							break;
						case listen_bpm_group:
							StaticData.bpmGroups = this.databaseService.findAll(null, BpmGroupDTO.class);
							break;

						}
					} else if (nameNotification.contains(NOTIFICATION_LISTEN)) {
						NotificationListen notificationListen = StaticData.JSONB.fromJson(notification.getParameter(),
								NotificationListen.class);
						if (StaticData.websocketSession.get(notificationListen.getUser()) == null)
							continue;

						for (Session session : StaticData.websocketSession.get(notificationListen.getUser()))
							NotificationWebSocket.sendMessage(session, notificationListen.getCount().toString());

					} else {

						String modelCode = nameNotification.substring(UpdateColumnModel.LISTEN_PREFIX.length());
						ModelDTO modelDTO = StaticData.models.stream().filter(a -> StringUtils.notNull(a.getCode()))
								.filter(a -> a.getCode().equals(modelCode)).findFirst().orElse(null);
						if (StringUtils.notNull(modelDTO)) {
							LoadStaticData.loadStaticDataCodebookModel(modelDTO.getId(), null);
						}
					}
				} catch (Exception e1) {
					logger.error(e1.getMessage(), e1);
				}

			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}

}
