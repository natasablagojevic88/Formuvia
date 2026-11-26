package rs.formuvia.bpm.entity;

import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bpm_item_previous", uniqueConstraints = @UniqueConstraint(columnNames = { "item",
		"parent" }, name = "bpm_item_previous_unique"), indexes = @Index(columnList = "item", name = "bpm_item_previous_index"))
public class BpmItemPrevious {

	@Id
	private UUID id;

	@JoinColumn(name = "item", nullable = false, foreignKey = @ForeignKey(name = "fk_bpm_item_previous_item"))
	@ManyToOne(cascade = CascadeType.REMOVE)
	private BpmItem item;

	@JoinColumn(name = "parent", nullable = false, foreignKey = @ForeignKey(name = "fk_bpm_item_previous_apremt"))
	@ManyToOne(cascade = CascadeType.REMOVE)
	private BpmItem parent;

	@Column(name = "previous_accept", nullable = false)
	private Boolean previousAccept;
}
