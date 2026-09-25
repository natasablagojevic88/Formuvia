package rs.formuvia.common.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.common.entity.Track;
import rs.formuvia.common.enums.TrackAction;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.utils.RoleList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityClass(value = Track.class, roles = RoleList.ADMIN)
public class TrackDTO {

	private UUID id;

	private String tableName;

	private UUID dataId;

	private TrackAction action;

	private LocalDateTime dateTime;

	private UUID appUserId;

	private String appUserUsername;

	private String appUserName;

	private String appUserSurname;

	private String ipAddress;

	private String oldData;

	private String newData;
}
