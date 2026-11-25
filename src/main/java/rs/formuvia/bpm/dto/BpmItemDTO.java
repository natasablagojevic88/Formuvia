package rs.formuvia.bpm.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.formuvia.bpm.entity.BpmItem;
import rs.formuvia.bpm.enums.BpmExecuteType;
import rs.formuvia.database.annotations.EntityClass;
import rs.formuvia.database.annotations.HideInTable;
import rs.formuvia.database.annotations.SkipColumn;
import rs.formuvia.utils.RoleList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityClass(value = BpmItem.class, roles = RoleList.ADMIN)
public class BpmItemDTO {

	private UUID id;

	@NotNull
	private UUID bpmModelId;

	@NotNull
	private String name;

	@NotNull
	private BpmExecuteType type;

	private UUID appUserId;

	private String procedure;

	private String mailSubject;

	private String mailBody;

	@NotNull
	private Boolean isRejectEnd;

	@NotNull
	private Boolean isAcceptEnd;

	private String sqlCondition;

	private Boolean previousAccept;

	private UUID previousItemId;

	@SkipColumn
	private List<BpmItemMailReceiveDTO> receiveNotification = new ArrayList<>();

	@SkipColumn
	private List<BpmItemChangeColumnDTO> changeColumns = new ArrayList<>();

	@SkipColumn
	@HideInTable
	private List<BpmItemDTO> children = new ArrayList<>();
}
