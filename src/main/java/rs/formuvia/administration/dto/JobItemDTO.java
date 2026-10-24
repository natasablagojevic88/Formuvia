package rs.formuvia.administration.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.administration.entity.Role;
import rs.formuvia.administration.entity.Smtp;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.database.annotations.InitSort;
import rs.formuvia.quartz.entity.JobItem;
import rs.formuvia.quartz.enums.JobExportType;
import rs.formuvia.quartz.enums.JobOutputType;
import rs.formuvia.quartz.enums.JobType;
import rs.formuvia.report.entity.Report;
import rs.formuvia.utils.RoleList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityClass(value = JobItem.class, roles = RoleList.ADMIN)
public class JobItemDTO {

	private UUID id;

	@NotNull
	@InitSort
	private String name;

	@NotNull
	private JobType jobType;

	private String receive;

	private Report reportId;

	private JobOutputType jobOutputType;

	private JobExportType exportType;

	private Boolean active;

	private String fileName;

	private Boolean conditional;

	private String mailSubject;

	private String mailBody;

	private String csvDelimiter;

	private String filePath;

	private Role roleId;

	private Smtp smtp;
}
