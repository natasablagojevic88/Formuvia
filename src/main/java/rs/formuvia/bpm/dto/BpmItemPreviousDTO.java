package rs.formuvia.bpm.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.bpm.entity.BpmItemPrevious;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.utils.RoleList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityClass(value = BpmItemPrevious.class, roles = RoleList.ADMIN)
public class BpmItemPreviousDTO {
	private UUID id;

	private UUID itemId;

	private UUID itemBpmModelId;

	private UUID parentId;

	private Boolean previousAccept;
}
