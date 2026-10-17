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
import rs.formuvia.model.enums.ModelTriggerAction;
import rs.formuvia.model.enums.ModelTriggerWhen;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "model_trigger", uniqueConstraints = { @UniqueConstraint(columnNames = { "model",
		"name" }, name = "model_trigger_unique1") }, indexes = @Index(columnList = "model", name = "model_trigger_model_index"))

public class ModelTrigger {

	@Id
	private UUID id;

	@JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "fk_model_trigger_model"))
	@ManyToOne(cascade = CascadeType.REMOVE)
	private Model model;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, name = "trigger_when")
	private ModelTriggerWhen triggerWhen;

	@Column(nullable = false)
	private ModelTriggerAction action;

	@Column
	private String condition;

	@Column(nullable = false, name = "function_name")
	private String functionName;
}
