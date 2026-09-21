package rs.formuvia.model.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.database.annotations.ComboboxList;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.database.annotations.HideInTable;
import rs.formuvia.database.annotations.SkipColumn;
import rs.formuvia.model.entity.Model;
import rs.formuvia.model.enums.ModelType;
import rs.formuvia.utils.DatabaseListen;
import rs.formuvia.utils.RoleList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityClass(value = Model.class, roles = RoleList.ADMIN)
public class ModelTreeDTO {

	@HideInTable
	private UUID id;

	@ComboboxList(DatabaseListen.model_listen)
	private UUID parentId;

	@NotNull
	private ModelType type;

	private String code;

	@NotNull
	private String name;

	private String description;

	private String icon;

	@ComboboxList(DatabaseListen.role_listen)
	private UUID previewRoleId;

	private String previewRoleCode;

	@ComboboxList(DatabaseListen.role_listen)
	private UUID addRoleId;

	private String addRoleCode;

	@ComboboxList(DatabaseListen.role_listen)
	private UUID updateRoleId;

	private String updateRoleCode;

	@ComboboxList(DatabaseListen.role_listen)
	private UUID deleteRoleId;

	private String deleteRoleCode;
	
	@SkipColumn
	private List<ModelTreeDTO> children = new ArrayList<>();
}
