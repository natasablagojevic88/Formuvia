package rs.formuvia.common.service;

import java.util.List;
import java.util.UUID;

import rs.formuvia.common.dto.NotificationDTO;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;

public interface NotificationService {

	Long getCount();

	List<NotificationDTO> getListUnseen();

	DatabaseTable<NotificationDTO> notificationListAll(DatabaseParameter databaseParameter);

	void getNotificationChange(UUID id, boolean seen);

}
