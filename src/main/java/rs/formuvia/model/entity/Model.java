package rs.formuvia.model.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.administration.entity.Role;
import rs.formuvia.model.enums.ModelType;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "model", uniqueConstraints = { @UniqueConstraint(columnNames = { "code" }, name = "model_code_unique"),
		@UniqueConstraint(columnNames = {
				"name" }, name = "model_name_unique") }, indexes = @Index(columnList = "parent", name = "model_parent_index"))
public class Model {

	@Id
	private UUID id;

	@JoinColumn(foreignKey = @ForeignKey(name = "fk_model_parent"))
	private Model parent;

	@Column(nullable = false)
	private ModelType type;

	@Column
	private String code;

	@Column(nullable = false)
	private String name;

	@Column
	private String description;

	@Column
	private String icon;

	@Column(name = "dialog_width")
	private Integer dialogWidth;

	@Column(name = "row_number")
	private Integer rowNumber;

	@Column(name = "column_number")
	private Integer columnNumber;

	@JoinColumn(name = "preview_role", foreignKey = @ForeignKey(name = "fk_model_preview_role"))
	private Role previewRole;

	@JoinColumn(name = "add_role", foreignKey = @ForeignKey(name = "fk_model_add_role"))
	private Role addRole;

	@JoinColumn(name = "update_role", foreignKey = @ForeignKey(name = "fk_model_update_role"))
	private Role updateRole;

	@JoinColumn(name = "delete_role", foreignKey = @ForeignKey(name = "fk_model_delete_role"))
	private Role deleteRole;

}
