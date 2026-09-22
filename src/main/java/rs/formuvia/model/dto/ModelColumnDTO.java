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
import rs.formuvia.database.annotations.InitSort;
import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.model.entity.ModelColumn;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.DatabaseListen;
import rs.formuvia.utils.RoleList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityClass(value=ModelColumn.class,roles = RoleList.ADMIN,saveUrl = ApiRoute.modelColumn)
public class ModelColumnDTO {

	@HideInTable
	private UUID id;
	
	@NotNull
	@ComboboxList(DatabaseListen.listen_model)
	@HideInTable
	private UUID modelId;
	
	@NotNull
	@InitSort
	private String code;
	
	@NotNull
	private String name;
	
	@NotNull
	private ColumnType columnType;

	@HideInTable
	private Integer length;
	
	@ComboboxList(DatabaseListen.listen_model)
	private UUID codebookId;
	
	@NotNull
	private Boolean nullable;
	
	@NotNull
	private Boolean editable;
	
	@HideInTable
	private String defaultValueSql;

}
