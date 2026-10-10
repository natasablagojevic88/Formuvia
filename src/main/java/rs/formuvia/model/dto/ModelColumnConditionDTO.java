package rs.formuvia.model.dto;

import java.util.UUID;

import com.drew.lang.annotations.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.database.annotations.InitSort;
import rs.formuvia.database.enums.Direction;
import rs.formuvia.database.enums.SearchOperation;
import rs.formuvia.model.entity.ModelColumnCondition;
import rs.formuvia.model.enums.ModelColumnConditionType;
import rs.formuvia.utils.RoleList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityClass(value = ModelColumnCondition.class, roles = RoleList.ADMIN)
public class ModelColumnConditionDTO {

	@InitSort(direction = Direction.DESC)
	private UUID id;

	@NotNull
	private UUID modelColumnId;

	@NotNull
	private ModelColumnConditionType type;

	@NotNull
	private UUID conditionColumnId;

	private String conditionColumnCode;

	private String conditionColumnName;

	@NotNull
	private SearchOperation searchOperation;

	private String field1;

	private String field2;
}
