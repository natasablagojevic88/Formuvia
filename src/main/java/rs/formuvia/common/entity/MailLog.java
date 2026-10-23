package rs.formuvia.common.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.administration.entity.Smtp;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mail_log")
public class MailLog {

	@Id
	private UUID id;

	@JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "fk_mail_log_smtp"))
	private Smtp smtp;

	@Column(nullable = false, columnDefinition = "text")
	private String receive;

	@Column(nullable = false)
	private String subject;

	@Column(name = "mail_text", columnDefinition = "text")
	private String mailText;

	@Column(nullable = false)
	private Boolean sent;

	@Column(name = "error_text", columnDefinition = "text")
	private String errorText;

	@Column(name = "send_time", nullable = false)
	private LocalDateTime sendTime;

}
