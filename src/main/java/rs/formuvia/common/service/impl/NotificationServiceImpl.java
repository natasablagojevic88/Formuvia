package rs.formuvia.common.service.impl;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Inject;
import rs.formuvia.common.dto.NotificationDTO;
import rs.formuvia.common.entity.Notification;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.NotificationService;
import rs.formuvia.common.service.ResourceBundleService;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.exceptions.ForbiddenException;

@Service
public class NotificationServiceImpl implements NotificationService {

	@Inject
	private DatabaseService databaseService;

	@Inject
	private CommonService commonService;

	@Inject
	private ResourceBundleService resourceBundleService;

	private final String NOTIFICATION_UNREAD_FILE = "countUnreadNotification.sql";

	@Override
	public Long getCount() {
		Map<Integer, Object> parameters = Map.of(1, commonService.getUser().getId());
		return databaseService
				.executeNativeQuery(commonService.readQueryFromFile(NOTIFICATION_UNREAD_FILE), parameters, Long.class)
				.getFirst();
	}

	@Override
	public List<NotificationDTO> getListUnseen() {
		DatabaseParameter databaseParameter = DatabaseParameter.valueOf(
				new DatabaseFilter[] { DatabaseFilter.valueOf("appUserId", commonService.getUser().getId().toString()),
						DatabaseFilter.valueOf("seen", "false") });
		List<NotificationDTO> list = this.databaseService.findAll(databaseParameter, NotificationDTO.class);
		list.forEach(a -> {
			a.setBody(this.resourceBundleService.getText(a.getBody()));
			a.setTitle(this.resourceBundleService.getText(a.getTitle()));
		});
		return list;
	}

	@Override
	public DatabaseTable<NotificationDTO> notificationListAll(DatabaseParameter databaseParameter) {
		databaseParameter.getFilters()
				.add(DatabaseFilter.valueOf("appUserId", commonService.getUser().getId().toString()));
		DatabaseTable<NotificationDTO> database = this.databaseService.createTable(databaseParameter,
				NotificationDTO.class);
		database.getList().forEach(a -> {
			a.setBody(this.resourceBundleService.getText(a.getBody()));
			a.setTitle(this.resourceBundleService.getText(a.getTitle()));
		});
		return database;
	}

	@Override
	public void getNotificationChange(UUID id, boolean seen) {
		Notification notification = this.databaseService.findById(id, Notification.class);
		if (!notification.getAppUser().getId().equals(commonService.getUser().getId()))
			throw new ForbiddenException();

		notification.setSeen(seen);
		this.databaseService.save(notification);

	}

}
