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
import rs.formuvia.model.entity.ModelColumn;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bpm_item_change_column", uniqueConstraints = @UniqueConstraint(name = "bpm_item_change_column_unique1", columnNames = {
		"bpm_item", "model_column" }), indexes = @Index(columnList = "bpm_item", name = "bpm_item_change_column_index"))
public class BpmItemChangeColumn {

	@Id
	private UUID id;

	@JoinColumn(name = "bpm_item", nullable = false, foreignKey = @ForeignKey(name = "fk_bpm_item_change_column_item"))
	@ManyToOne(cascade = CascadeType.REMOVE)
	private BpmItem bpmItem;

	@JoinColumn(name = "model_column", nullable = false, foreignKey = @ForeignKey(name = "fk_bpm_item_change_column_column"))
	@ManyToOne(cascade = CascadeType.REMOVE)
	private ModelColumn modelColumn;

}
