package rs.formuvia.common.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "log_table")
public class LogTable {

	@Id
	private UUID id;

	@Column(name = "log_time", nullable = false)
	private LocalDateTime logTime;

	@Column
	private String level;

	@Column
	private String logger;

	@Column(columnDefinition = "text")
	private String message;

	@Column(columnDefinition = "text")
	private String exception;
}
