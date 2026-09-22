package rs.formuvia.model.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.database.annotations.ComboboxList;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.database.annotations.HideInTable;
import rs.formuvia.model.entity.Model;
import rs.formuvia.model.enums.ModelType;
import rs.formuvia.utils.DatabaseListen;
import rs.formuvia.utils.RoleList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityClass(value = Model.class, roles = RoleList.ADMIN)
public class ModelDTO {

	@HideInTable
	private UUID id;

	@ComboboxList(DatabaseListen.listen_model)
	private UUID parentId;

	@NotNull
	private ModelType type;

	private String code;

	@NotNull
	private String name;

	private String description;

	private String icon;

	@ComboboxList(DatabaseListen.listen_role)
	private UUID previewRoleId;
	
	private String previewRoleCode;

	@ComboboxList(DatabaseListen.listen_role)
	private UUID addRoleId;
	
	private String addRoleCode;

	@ComboboxList(DatabaseListen.listen_role)
	private UUID updateRoleId;
	
	private String updateRoleCode;

	@ComboboxList(DatabaseListen.listen_role)
	private UUID deleteRoleId;
	
	private String deleteRoleCode;
}
