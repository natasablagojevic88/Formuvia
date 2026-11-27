package rs.formuvia.bpm.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.administration.dto.AppUserDTO;
import rs.formuvia.bpm.entity.BpmGroup;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.database.annotations.HideInTable;
import rs.formuvia.database.annotations.InitSort;
import rs.formuvia.database.annotations.NotEditableInTable;
import rs.formuvia.database.annotations.SkipColumn;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.RoleList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityClass(value = BpmGroup.class, roles = RoleList.ADMIN, saveUrl = ApiRoute.bpmGroup)
public class BpmGroupDTO {

	@Id
	@NotEditableInTable
	@HideInTable
	private UUID id;

	@Column(nullable = false)
	@InitSort
	private String name;

	@HideInTable
	@SkipColumn
	@NotEditableInTable
	private List<AppUserDTO> allUsers = new ArrayList<>();

	@HideInTable
	@SkipColumn
	@NotEditableInTable
	private List<AppUserDTO> users = new ArrayList<>();

}
