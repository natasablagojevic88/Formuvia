package rs.formuvia.quartz.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.quartz.enums.JobLogStatus;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "job_log")
public class JobLog {

	@Id
	private UUID id;

	@JoinColumn(name = "job_item", nullable = false, foreignKey = @ForeignKey(name = "fk_job_log_job_item"))
	@ManyToOne(cascade = CascadeType.REMOVE)
	private JobItem jobItem;

	@Column(name = "start_time", nullable = false)
	private LocalDateTime startTime;

	@Column(name = "end_time")
	private LocalDateTime endTime;

	@Column(nullable = false)
	private JobLogStatus status;

	@Column(name = "error_text", columnDefinition = "text")
	private String errorText;

}
