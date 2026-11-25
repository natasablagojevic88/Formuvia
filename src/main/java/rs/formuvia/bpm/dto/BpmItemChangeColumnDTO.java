package rs.formuvia.bpm.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.bpm.entity.BpmItemChangeColumn;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.utils.RoleList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityClass(value = BpmItemChangeColumn.class, roles = RoleList.ADMIN)
public class BpmItemChangeColumnDTO {

	private UUID id;

	private UUID bpmItemId;

	private UUID bpmItemBpmModelId;

	private UUID modelColumnId;

}
