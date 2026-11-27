package rs.formuvia.bpm.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.bpm.entity.BpmModel;
import rs.formuvia.database.annotations.ComboboxList;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.database.annotations.HideInTable;
import rs.formuvia.database.annotations.InitSort;
import rs.formuvia.database.annotations.SkipColumn;
import rs.formuvia.utils.DatabaseListen;
import rs.formuvia.utils.RoleList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityClass(value = BpmModel.class, roles = RoleList.ADMIN)
public class BpmModelDTO {

	@HideInTable
	private UUID id;

	@HideInTable
	@NotNull
	private UUID modelId;

	@InitSort
	@NotNull
	private String name;

	private String startSqlProcedure;

	private String endSqlProcedure;

	@NotNull
	@HideInTable
	@ComboboxList(DatabaseListen.listen_smtp)
	private UUID smtpId;

	@SkipColumn
	@HideInTable
	private List<BpmItemDTO> items = new ArrayList<>();

}
