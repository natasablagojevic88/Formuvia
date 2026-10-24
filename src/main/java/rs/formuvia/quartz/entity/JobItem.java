package rs.formuvia.quartz.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.administration.entity.Role;
import rs.formuvia.administration.entity.Smtp;
import rs.formuvia.quartz.enums.JobExportType;
import rs.formuvia.quartz.enums.JobOutputType;
import rs.formuvia.quartz.enums.JobType;
import rs.formuvia.report.entity.Report;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "job_item", uniqueConstraints = @UniqueConstraint(columnNames = {
		"name" }, name = "job_item_name_unique"))
public class JobItem {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String name;

	@Column(name = "job_type", nullable = false)
	private JobType jobType;

	@Column
	private String receive;

	@JoinColumn(foreignKey = @ForeignKey(name = "fk_job_item_report"))
	private Report report;

	@Column(name = "job_output_type")
	private JobOutputType jobOutputType;

	@Column(name = "export_type")
	private JobExportType exportType;

	@Column(nullable = false)
	private Boolean active;

	@Column(name = "file_name")
	private String fileName;

	@Column(nullable = false)
	private Boolean conditional;

	@Column(name = "mail_subject")
	private String mailSubject;

	@Column(name = "mail_body", columnDefinition = "text")
	private String mailBody;

	@Column(name = "csv_delimiter")
	private String csvDelimiter;

	@Column(name = "file_path")
	private String filePath;

	@JoinColumn(foreignKey = @ForeignKey(name = "fk_job_item_role"))
	private Role role;

	@JoinColumn(foreignKey = @ForeignKey(name = "fk_job_item_smtp"))
	private Smtp smtp;
}
