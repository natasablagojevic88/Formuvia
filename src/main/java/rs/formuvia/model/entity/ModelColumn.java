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
import rs.formuvia.database.enums.ColumnType;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="model_column",
	uniqueConstraints = {@UniqueConstraint(columnNames = { "model","code" },name="model_column_unique1")},
	indexes = @Index(columnList = "model",name="model_column_model_index")
		)
public class ModelColumn {

	@Id
	private UUID id;
	
	@JoinColumn(nullable = false,foreignKey = @ForeignKey(name="fk_model_column_model"))
	@ManyToOne(cascade = CascadeType.REMOVE)
	private Model model;
	
	@Column(nullable = false)
	private String code;
	
	@Column(nullable = false)
	private String name;
	
	@Column(name="column_type", nullable = false)
	private ColumnType columnType;
	
	@Column
	private Integer length;
	
	@JoinColumn(foreignKey = @ForeignKey(name="fk_model_column_codebook"))
	private Model codebook;
	
	@Column(nullable = false)
	private Boolean nullable;
	
	@Column(nullable = false)
	private Boolean editable;
	
	@Column(name="default_value_sql")
	private String defaultValueSql;
	
	@Column(name="text_area",nullable = false)
	private Boolean textArea;
	
	@Column(nullable = false)
	private Integer row;
	
	@Column(nullable = false)
	private Integer column;
	
	@Column(nullable = false)
	private Integer colspan;

}
