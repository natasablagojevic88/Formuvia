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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.database.enums.SearchOperation;
import rs.formuvia.model.enums.ModelColumnConditionType;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "model_column_condition", indexes = @Index(columnList = "model_column", name = "model_column_condition_column_index"))
public class ModelColumnCondition {

	@Id
	private UUID id;

	@JoinColumn(name = "model_column", nullable = false, foreignKey = @ForeignKey(name = "fk_model_column_condition_column"))
	@ManyToOne(cascade = CascadeType.REMOVE)
	private ModelColumn modelColumn;

	@Column(nullable = false)
	private ModelColumnConditionType type;

	@JoinColumn(name = "condition_column", nullable = false, foreignKey = @ForeignKey(name = "fk_model_column_condition_condition"))
	private ModelColumn conditionColumn;

	@Column(name = "search_operation", nullable = false)
	private SearchOperation searchOperation;

	@Column
	private String field1;

	@Column
	private String field2;
}
