package rs.formuvia.bpm.utils;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;

import rs.formuvia.bpm.dto.BpmItemChangeColumnDTO;
import rs.formuvia.bpm.dto.BpmItemDTO;
import rs.formuvia.bpm.dto.BpmItemMailReceiveDTO;
import rs.formuvia.bpm.dto.BpmItemPreviousDTO;
import rs.formuvia.bpm.dto.BpmModelDTO;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.ExecuteQuery;

public class CreateBpmModelDTO implements ExecuteQuery<BpmModelDTO> {
	private final UUID modelId;

	private DatabaseService databaseService = new DatabaseServiceImpl();

	public CreateBpmModelDTO(UUID modelId) {
		this.modelId = modelId;
	}

	@Override
	public BpmModelDTO execute(Connection connection) throws Exception {
		BpmModelDTO bpmModelDTO = this.databaseService.findById(modelId, BpmModelDTO.class, connection);

		List<BpmItemDTO> list = this.databaseService.findAll(
				DatabaseParameter.valueOf(DatabaseFilter.valueOf("bpmModelId", modelId.toString())), BpmItemDTO.class,
				connection);

		List<BpmItemMailReceiveDTO> listReceive = this.databaseService.findAll(
				DatabaseParameter.valueOf(DatabaseFilter.valueOf("bpmItemBpmModelId", modelId.toString())),
				BpmItemMailReceiveDTO.class, connection);

		List<BpmItemChangeColumnDTO> listChangeColumn = this.databaseService.findAll(
				DatabaseParameter.valueOf(DatabaseFilter.valueOf("bpmItemBpmModelId", modelId.toString())),
				BpmItemChangeColumnDTO.class, connection);

		List<BpmItemPreviousDTO> previousDTOs = this.databaseService.findAll(
				DatabaseParameter.valueOf(DatabaseFilter.valueOf("itemBpmModelId", modelId.toString())),
				BpmItemPreviousDTO.class, connection);

		for (BpmItemDTO item : list.stream()
				.filter(a -> !previousDTOs.stream().map(b -> b.getItemId()).toList().contains(a.getId())).toList()) {
			addReceive(item, listReceive);
			addChangeColumn(item, listChangeColumn);
			addChilden(list, item, listReceive, listChangeColumn, previousDTOs);
			bpmModelDTO.getItems().add(item);

		}
		return bpmModelDTO;
	}

	private void addChilden(List<BpmItemDTO> list, BpmItemDTO parent, List<BpmItemMailReceiveDTO> listReceive,
			List<BpmItemChangeColumnDTO> listChangeColumn, List<BpmItemPreviousDTO> previousDTOs) {

		List<BpmItemPreviousDTO> children = previousDTOs.stream().filter(a -> a.getParentId().equals(parent.getId()))
				.toList();

		for (BpmItemPreviousDTO childPrevious : children) {
			BpmItemDTO child = list.stream().filter(a -> a.getId().equals(childPrevious.getItemId())).findFirst().get();
			child.setPreviousAccept(childPrevious.getPreviousAccept());
			addReceive(child, listReceive);
			addChangeColumn(child, listChangeColumn);
			parent.getChildren().add(child);
			addChilden(list, child, listReceive, listChangeColumn, previousDTOs);
		}

	}

	private void addReceive(BpmItemDTO item, List<BpmItemMailReceiveDTO> listReceive) {
		item.setReceiveNotification(listReceive.stream().filter(a -> a.getBpmItemId().equals(item.getId())).toList());
	}

	private void addChangeColumn(BpmItemDTO item, List<BpmItemChangeColumnDTO> listChangeColumn) {
		item.setChangeColumns(listChangeColumn.stream().filter(a -> a.getBpmItemId().equals(item.getId())).toList());
	}

}
