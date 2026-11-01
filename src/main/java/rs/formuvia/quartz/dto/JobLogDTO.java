package rs.formuvia.quartz.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.database.annotations.HideInTable;
import rs.formuvia.database.annotations.InitSort;
import rs.formuvia.database.enums.Direction;
import rs.formuvia.quartz.entity.JobLog;
import rs.formuvia.quartz.enums.JobLogStatus;
import rs.formuvia.utils.RoleList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityClass(value = JobLog.class, roles = RoleList.ADMIN)
public class JobLogDTO {

	@HideInTable
	private UUID id;

	@HideInTable
	private UUID jobItemId;

	@InitSort(direction = Direction.DESC)
	private LocalDateTime startTime;

	private LocalDateTime endTime;

	private JobLogStatus status;

	private String errorText;

}
