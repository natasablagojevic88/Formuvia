package rs.formuvia.quartz.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "job_cron")
public class JobCron {

	@Id
	private UUID id;

	@Column(name = "any_minutes", nullable = false)
	private Boolean anyMinutes;

	@Column(name = "every_minute")
	private Integer everyMinute;

	@Column(name = "in_minutes")
	private String inMinutes;

	@Column(name = "any_hours", nullable = false)
	private Boolean anyHours;

	@Column(name = "every_hours")
	private Integer everyHours;

	@Column(name = "in_hours")
	private String inHours;

	@Column(name = "any_day", nullable = false)
	private Boolean anyDay;

	@Column(name = "every_day")
	private Integer everyDay;

	@Column(name = "in_day")
	private String inDay;
}
