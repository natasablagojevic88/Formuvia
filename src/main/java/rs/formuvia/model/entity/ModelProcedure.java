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
@Table(name = "model_procedure", uniqueConstraints = @UniqueConstraint(name = "model_procedure_name_unique", columnNames = {
		"name", "model" }), indexes = @Index(columnList = "model", name = "model_procedure_model_index"))
public class ModelProcedure {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String name;

	@JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "fk_model_procedure_model"))
	@ManyToOne(cascade = CascadeType.REMOVE)
	private Model model;

	@Column
	private String icon;

	@Column
	private String procedure;
}