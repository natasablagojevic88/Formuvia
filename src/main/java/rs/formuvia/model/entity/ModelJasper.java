package rs.formuvia.model.entity;

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
@Table(name = "model_jasper", uniqueConstraints = @UniqueConstraint(name = "model_jasper_unique1", columnNames = {
		"model", "name" }), indexes = @Index(columnList = "model", name = "model_jasper_model_index"))
public class ModelJasper {

	@Id
	private UUID id;

	@JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "fk_model_jasper_model"))
	@ManyToOne(cascade = CascadeType.REMOVE)
	private Model model;

	@Column(nullable = false)
	private String name;

	@JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "fk_model_jasper_file"))
	private ModelFile file;
}
