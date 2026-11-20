package rs.formuvia.common.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.common.entity.Notification;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.database.annotations.HideInTable;
import rs.formuvia.database.annotations.InitSort;
import rs.formuvia.database.enums.Direction;
import rs.formuvia.utils.RoleList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityClass(value = Notification.class, roles = RoleList.ADMIN)
public class NotificationDTO {

	@InitSort(direction = Direction.DESC, orderNumber = 2)
	@HideInTable
	private UUID id;

	@HideInTable
	private UUID appUserId;

	@InitSort(direction = Direction.ASC)
	private Boolean seen;

	private LocalDateTime notificationDate;

	private String title;

	private String body;

	private String url;
}
