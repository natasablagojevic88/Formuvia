package rs.formuvia.common.entity;

import java.time.LocalDateTime;
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
import rs.formuvia.administration.entity.AppUser;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="token",
	uniqueConstraints = {@UniqueConstraint(name="token_access_token_unique", columnNames = { "access_token" }),
			@UniqueConstraint(name="token_refresh_token_unique", columnNames = { "refresh_token" })
		}
		)
public class Token {

	@Id
	private UUID id;
	
	@JoinColumn(name="appuser",nullable = false,foreignKey = @ForeignKey(name="fk_token_appuser"))
	private AppUser appUser;
	
	@Column(name="access_token",nullable = false)
	private String accessToken;
	
	@Column(name="access_token_exipires",nullable = false)
	private LocalDateTime accessTokenExipires;
	
	@Column(name="refresh_token",nullable = false)
	private String refreshToken;
	
	@Column(name="refresh_token_exipires",nullable = false)
	private LocalDateTime refreshTokenExipires;
	
	@Column(nullable = false)
	private Boolean active = true;
}
