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
import rs.formuvia.model.entity.Model;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bpm_model", uniqueConstraints = @UniqueConstraint(name = "bpm_model_unique_name", columnNames = { "name",
		"model" }))
public class BpmModel {

	@Id
	private UUID id;

	@JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "fk_bpm_model_model"))
	@ManyToOne(cascade = CascadeType.REMOVE)
	private Model model;

	@Column(nullable = false)
	private String name;

	@Column(name = "start_sql_procedure")
	private String startSqlProcedure;

	@Column(name = "end_sql_procedure")
	private String endSqlProcedure;
}
