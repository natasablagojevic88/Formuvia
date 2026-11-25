package rs.formuvia.bpm.entity;

import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.administration.entity.AppUser;
import rs.formuvia.bpm.enums.BpmExecuteType;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bpm_item", uniqueConstraints = @UniqueConstraint(columnNames = { "bpm_model",
		"name" }, name = "bpm_item_unique1"))
public class BpmItem {

	@Id
	private UUID id;

	@JoinColumn(name = "bpm_model", nullable = false, foreignKey = @ForeignKey(name = "fk_bpm_item_bpm_model"))
	@ManyToOne(cascade = CascadeType.REMOVE)
	private BpmModel bpmModel;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private BpmExecuteType type;

	@JoinColumn(name = "app_user", foreignKey = @ForeignKey(name = "fk_bpm_item_app_user"))
	private AppUser appUser;

	@Column
	private String procedure;

	@Column(name = "mail_subject")
	private String mailSubject;

	@Column(name = "mail_body", columnDefinition = "text")
	private String mailBody;

	@Column(name = "is_reject_end", nullable = false)
	private Boolean isRejectEnd;

	@Column(name = "is_accept_end", nullable = false)
	private Boolean isAcceptEnd;

	@Column(name = "sql_condition", columnDefinition = "text")
	private String sqlCondition;

	@Column(name = "previous_accept", nullable = false)
	private Boolean previousAccept;

	@JoinColumn(name = "previous_item", foreignKey = @ForeignKey(name = "fk_bpm_item_previous_item"))
	@ManyToOne(cascade = CascadeType.REMOVE)
	private BpmItem previousItem;

}
