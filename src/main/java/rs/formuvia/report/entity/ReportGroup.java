package rs.formuvia.report.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import rs.formuvia.administration.entity.Role;

@Setter
@Getter
@Entity
@Table(name = "report_group", uniqueConstraints = @UniqueConstraint(name = "report_group_name_unique", columnNames = {
		"name" }))
public class ReportGroup {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String name;

	@JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "fk_report_group_role"))
	private Role role;

	@Column
	private String icon;
}
