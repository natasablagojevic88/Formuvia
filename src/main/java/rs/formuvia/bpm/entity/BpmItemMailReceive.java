package rs.formuvia.bpm.entity;

import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "bpm_item_mail_receive", uniqueConstraints = @UniqueConstraint(name = "bpm_item_mail_receive_unique1", columnNames = {
		"bpm_item", "app_user" }), indexes = @Index(columnList = "bpm_item", name = "bpm_item_mail_receive_index"))
public class BpmItemMailReceive {

	@Id
	private UUID id;

	@JoinColumn(name = "bpm_item", nullable = false, foreignKey = @ForeignKey(name = "fk_bpm_item_mail_receive_item"))
	@ManyToOne(cascade = CascadeType.REMOVE)
	private BpmItem bpmItem;

	@JoinColumn(name = "app_user", nullable = false, foreignKey = @ForeignKey(name = "fk_bpm_item_mail_receive_user"))
	private AppUser appUser;

}
