package rs.formuvia.quartz.entity;

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

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "job_item_webhook")
public class JobItemWebhook {

	@Id
	private UUID id;

	@JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "fk_job_item_webhook_job"))
	@ManyToOne(cascade = CascadeType.REMOVE)
	private JobItem job;

	@Column(name = "bearer_token", nullable = false)
	private String bearerToken;

	@Column
	private String description;

}
