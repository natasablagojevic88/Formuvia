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
import rs.formuvia.report.enums.ReportType;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "report", uniqueConstraints = @UniqueConstraint(columnNames = { "report_group",
		"name" }, name = "report_unique1"), indexes = @Index(columnList = "report_group", name = "report_group_index"))
public class Report {

	@Id
	private UUID id;

	@JoinColumn(name = "report_group", nullable = false, foreignKey = @ForeignKey(name = "fk_report_group"))
	@ManyToOne(cascade = CascadeType.REMOVE)
	private ReportGroup reportGroup;

	@Column(nullable = false)
	private ReportType type;

	@Column(nullable = false)
	private String name;

	@Column
	private String description;

	@Column(name = "sql_query", columnDefinition = "text")
	private String sqlQuery;
}
