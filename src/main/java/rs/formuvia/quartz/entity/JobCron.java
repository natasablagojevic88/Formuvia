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

	@Column(name = "any_month", nullable = false)
	private Boolean anyMonth;

	@Column(name = "every_month")
	private Integer everyMonth;

	@Column(name = "in_month")
	private String inMonth;

	@Column(name = "any_d_of_week", nullable = false)
	private Boolean anyDOfWeek;

	@Column(name = "in_d_of_week")
	private String inDOfWeek;

	@Column(name = "any_year", nullable = false)
	private Boolean anyYear;

	@Column(name = "every_year")
	private Integer everyYear;

	@Column(name = "in_year")
	private String inYear;

}
