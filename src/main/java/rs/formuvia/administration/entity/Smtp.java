package rs.formuvia.administration.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.common.enums.SmtpSecurity;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "smtp", uniqueConstraints = @UniqueConstraint(columnNames = { "name" }, name = "smtp_unique_name"))
public class Smtp {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String sender;

	@Column(nullable = false)
	private String host;

	@Column(nullable = false)
	private Integer port;

	@Column(nullable = false)
	private Boolean auth;

	@Column
	private String username;

	@Column
	private String password;

	@Column(name = "smtp_security", nullable = false)
	private SmtpSecurity smtpSecurity;

}
