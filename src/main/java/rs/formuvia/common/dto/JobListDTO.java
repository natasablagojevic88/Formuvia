package rs.formuvia.common.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.database.annotations.HideInTable;
import rs.formuvia.quartz.enums.JobLogStatus;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class JobListDTO {

	private String name;

	private Boolean active;

	private LocalDateTime previousFire;

	private LocalDateTime nextFire;

	private JobLogStatus status;

	private LocalDateTime lastEndDate;

	private String errorText;

	@HideInTable
	private UUID jobId;

	@HideInTable
	private UUID reportId;
}
