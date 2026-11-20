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
import rs.formuvia.administration.entity.AppUser;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notification")
public class Notification {

	@Id
	private UUID id;

	@JoinColumn(name = "app_user", nullable = false, foreignKey = @ForeignKey(name = "fk_notification_app_user"))
	private AppUser appUser;

	@Column(nullable = false)
	private Boolean seen;

	@Column(name = "notification_date", nullable = false)
	private LocalDateTime notificationDate;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false, columnDefinition = "text")
	private String body;

	@Column
	private String url;
}
