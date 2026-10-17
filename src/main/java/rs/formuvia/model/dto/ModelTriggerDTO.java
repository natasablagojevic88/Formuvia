package rs.formuvia.model.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.model.entity.ModelTrigger;
import rs.formuvia.model.enums.ModelTriggerAction;
import rs.formuvia.model.enums.ModelTriggerWhen;
import rs.formuvia.utils.RoleList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityClass(value = ModelTrigger.class, roles = RoleList.ADMIN)
public class ModelTriggerDTO {

	private UUID id;

	@NotNull
	private UUID modelId;

	@NotNull
	private String name;

	@NotNull
	private ModelTriggerWhen triggerWhen;

	@NotNull
	private ModelTriggerAction action;

	private String condition;

	@NotNull
	private String functionName;
}
