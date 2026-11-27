package rs.formuvia.bpm.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.bpm.entity.BpmGroupUser;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.utils.RoleList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityClass(value = BpmGroupUser.class, roles = RoleList.ADMIN)
public class BpmGroupUserDTO {

	private UUID id;

	private UUID userGroupId;

	private UUID appUserId;
}
