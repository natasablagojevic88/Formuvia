package rs.formuvia.quartz.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.database.annotations.ComboboxList;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.database.annotations.HideInTable;
import rs.formuvia.database.annotations.InitSort;
import rs.formuvia.database.annotations.NotEditableInTable;
import rs.formuvia.quartz.entity.JobItem;
import rs.formuvia.quartz.enums.JobExportType;
import rs.formuvia.quartz.enums.JobOutputType;
import rs.formuvia.quartz.enums.JobType;
import rs.formuvia.utils.DatabaseListen;
import rs.formuvia.utils.RoleList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityClass(value = JobItem.class, roles = RoleList.ADMIN)
public class JobItemDTO {

	@HideInTable
	@NotEditableInTable
	private UUID id;

	@NotNull
	@InitSort
	private String name;

	@NotNull
	private JobType jobType;

	private String receive;

	@HideInTable
	@NotEditableInTable
	private UUID reportId;

	@HideInTable
	private JobOutputType jobOutputType;

	@HideInTable
	private JobExportType exportType;

	@NotNull
	private Boolean active;

	@HideInTable
	private String fileName;

	@NotNull
	@HideInTable
	private Boolean conditional;

	@HideInTable
	private String mailSubject;

	@NotEditableInTable
	@HideInTable
	private String mailBody;

	@HideInTable
	private String csvDelimiter;

	@HideInTable
	private String filePath;

	@HideInTable
	@ComboboxList(DatabaseListen.listen_role)
	private UUID roleId;

	@HideInTable
	@ComboboxList(DatabaseListen.listen_smtp)
	private UUID smtpId;

	@HideInTable
	private String errorReceive;

	@NotEditableInTable
	@HideInTable
	private UUID jobCronId;

	@NotNull
	@NotEditableInTable
	@HideInTable
	private Boolean jobCronAnyMinutes;

	@NotEditableInTable
	@HideInTable
	private Integer jobCronEveryMinute;

	@NotEditableInTable
	@HideInTable
	private String jobCronInMinutes;

	@NotNull
	@NotEditableInTable
	@HideInTable
	private Boolean jobCronAnyHours;

	@NotEditableInTable
	@HideInTable
	private Integer jobCronEveryHours;

	@NotEditableInTable
	@HideInTable
	private String jobCronInHours;

	@NotNull
	@NotEditableInTable
	@HideInTable
	private Boolean jobCronAnyDay;

	@NotEditableInTable
	@HideInTable
	private Integer jobCronEveryDay;

	@NotEditableInTable
	@HideInTable
	private String jobCronInDay;

	@NotNull
	@NotEditableInTable
	@HideInTable
	private Boolean jobCronAnyMonth;

	@NotEditableInTable
	@HideInTable
	private Integer jobCronEveryMonth;

	@NotEditableInTable
	@HideInTable
	private String jobCronInMonth;

	@NotNull
	@NotEditableInTable
	@HideInTable
	private Boolean jobCronAnyDOfWeek;

	@NotEditableInTable
	@HideInTable
	private String jobCronInDOfWeek;

	@NotNull
	@NotEditableInTable
	@HideInTable
	private Boolean jobCronAnyYear;

	@NotEditableInTable
	@HideInTable
	private Integer jobCronEveryYear;

	@NotEditableInTable
	@HideInTable
	private String jobCronInYear;
}
