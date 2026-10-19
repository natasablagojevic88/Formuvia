package rs.formuvia.report.entity;

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
@Table(name = "report_parameter", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "report", "code" }, name = "report_parameter_unique1"),
		@UniqueConstraint(columnNames = { "report",
				"order_number" }, name = "report_parameter_unique2") }, indexes = @Index(columnList = "report", name = "report_parameter_report_index"))
public class ReportParameter {

	@Id
	private UUID id;

	@JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "fk_report_parameter_report"))
	@ManyToOne(cascade = CascadeType.REMOVE)
	private Report report;

	@Column(name = "order_number", nullable = false)
	private Integer orderNumber;

	@Column(name = "column_type", nullable = false)
	private ColumnType columnType;

	@Column(nullable = false)
	private String code;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private Boolean nullable;

	@Column(name = "default_value")
	private String defaultValue;

	@Column(name = "list_of_values", columnDefinition = "text")
	private String listOfValues;
}
