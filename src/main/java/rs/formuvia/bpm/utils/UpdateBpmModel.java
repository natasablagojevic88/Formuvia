package rs.formuvia.bpm.utils;

import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import rs.formuvia.bpm.dto.BpmItemChangeColumnDTO;
import rs.formuvia.bpm.dto.BpmItemDTO;
import rs.formuvia.bpm.dto.BpmItemMailReceiveDTO;
import rs.formuvia.bpm.dto.BpmModelDTO;
import rs.formuvia.bpm.entity.BpmItem;
import rs.formuvia.bpm.entity.BpmItemChangeColumn;
import rs.formuvia.bpm.entity.BpmItemMailReceive;
import rs.formuvia.bpm.entity.BpmModel;
import rs.formuvia.bpm.enums.BpmExecuteType;
import rs.formuvia.database.enums.SearchOperation;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.exceptions.UniqueException;
import rs.formuvia.model.service.impl.ModelServiceImpl;
import rs.formuvia.model.utils.UpdateColumnModel;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

public class UpdateBpmModel implements ExecuteQuery<BpmModelDTO> {
	private final BpmModelDTO bpmModelDTO;

	private DatabaseService databaseService = new DatabaseServiceImpl();
	private ModelMapper modelMapper = new ModelMapper();

	public UpdateBpmModel(BpmModelDTO bpmModelDTO) {
		this.bpmModelDTO = bpmModelDTO;
	}

	@Override
	public BpmModelDTO execute(Connection connection) throws Exception {
		if (StringUtils.hasText(bpmModelDTO.getStartSqlProcedure()))
			ModelServiceImpl.checkProcedureExists(bpmModelDTO.getStartSqlProcedure());

		if (StringUtils.hasText(bpmModelDTO.getEndSqlProcedure()))
			ModelServiceImpl.checkProcedureExists(bpmModelDTO.getEndSqlProcedure());

		DatabaseParameter uniqueParameter = DatabaseParameter.singleFilter(
				new DatabaseFilter[] { DatabaseFilter.valueOf("modelId", bpmModelDTO.getModelId().toString()),
						DatabaseFilter.valueOf("name", bpmModelDTO.getName()) });

		if (StringUtils.notNull(bpmModelDTO.getId()))
			uniqueParameter.getFilters()
					.add(DatabaseFilter.valueOf("id", SearchOperation.NOT_EQUALS, bpmModelDTO.getId().toString()));

		if (databaseService.exists(uniqueParameter, BpmModelDTO.class, connection))
			throw new UniqueException(UniqueException.findFieldFromList(BpmModelDTO.class, "name"),
					bpmModelDTO.getName());

		BpmModel bpmModel = bpmModelDTO.getId() == null ? new BpmModel()
				: this.databaseService.findById(bpmModelDTO.getId(), BpmModel.class, connection);
		this.modelMapper.map(bpmModelDTO, bpmModel);
		bpmModel = this.databaseService.save(bpmModel, connection);

		List<BpmItem> currentItem = this.databaseService.findAll(
				DatabaseParameter.valueOf(DatabaseFilter.valueOf("bpmModel", bpmModel.getId().toString())),
				BpmItem.class, connection);
		List<BpmItem> items = new ArrayList<>();
		for (BpmItemDTO item : bpmModelDTO.getItems()) {
			addItem(item, items, connection, bpmModel);
		}

		if (items.stream().filter(a -> a.getIsAcceptEnd()).count() == 0)
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noProcesFinished", null);

		for (BpmItem task : items) {

			List<BpmItem> children = findChildren(items, task.getId());

			if (!task.getIsAcceptEnd())
				if (children.stream().filter(a -> a.getPreviousAccept()).count() == 0)
					throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noTaskAcceptEnd", task.getName());

			if (!task.getType().equals(BpmExecuteType.TASK))
				continue;

			if (!task.getIsRejectEnd())
				if (children.stream().filter(a -> !a.getPreviousAccept()).count() == 0)
					throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noTaskRejectEnd", task.getName());

		}

		for (BpmItem itemToDelete : currentItem.stream()
				.filter(a -> !items.stream().map(b -> b.getId()).toList().contains(a.getId())).toList()) {
			this.databaseService.delete(itemToDelete, connection);
		}

		CreateBpmModelDTO createBpmModelDTO = new CreateBpmModelDTO(bpmModel.getId());
		return this.databaseService.executeQuery(createBpmModelDTO, connection);
	}

	private List<BpmItem> findChildren(List<BpmItem> items, UUID parentId) {
		return items.stream().filter(a -> a.getPreviousItem() != null)
				.filter(a -> a.getPreviousItem().getId().equals(parentId)).toList();
	}

	private void addItem(BpmItemDTO item, List<BpmItem> items, Connection connection, BpmModel bpmModel) {
		BpmItem bpmItem = new BpmItem();
		item.setBpmModelId(bpmModel.getId());
		if (item.getPreviousAccept() == null)
			item.setPreviousAccept(false);
		checkItem(item, connection);
		modelMapper.map(item, bpmItem);
		if (item.getPreviousItemId() != null) {
			BpmItem previousItem = new BpmItem();
			previousItem.setId(item.getPreviousItemId());
			bpmItem.setPreviousItem(previousItem);
		}
		bpmItem = this.databaseService.save(bpmItem, connection);

		List<BpmItemMailReceive> databaseReceives = this.databaseService.findAll(
				DatabaseParameter.valueOf(DatabaseFilter.valueOf("bpmItem", bpmItem.getId().toString())),
				BpmItemMailReceive.class, connection);
		for (BpmItemMailReceiveDTO receive : item.getReceiveNotification().stream().filter(
				a -> !databaseReceives.stream().map(b -> b.getId()).collect(Collectors.toList()).contains(a.getId()))
				.collect(Collectors.toList())) {
			BpmItemMailReceive receiveInsert = new BpmItemMailReceive();
			this.modelMapper.map(receive, receiveInsert);
			receiveInsert.setBpmItem(bpmItem);
			this.databaseService.save(receiveInsert, connection);
		}

		for (BpmItemMailReceive deleteReceive : databaseReceives
				.stream().filter(a -> !item.getReceiveNotification().stream()
						.filter(b -> StringUtils.notNull(b.getId())).map(b -> b.getId()).toList().contains(a.getId()))
				.toList()) {
			this.databaseService.delete(deleteReceive, connection);
		}

		List<BpmItemChangeColumn> databaseChangeColumns = this.databaseService.findAll(
				DatabaseParameter.valueOf(DatabaseFilter.valueOf("bpmItem", bpmItem.getId().toString())),
				BpmItemChangeColumn.class, connection);
		for (BpmItemChangeColumnDTO changeColumn : item.getChangeColumns().stream().filter(a -> !databaseChangeColumns
				.stream().map(b -> b.getId()).collect(Collectors.toList()).contains(a.getId()))
				.collect(Collectors.toList())) {
			BpmItemChangeColumn chengeColumnInsert = new BpmItemChangeColumn();
			this.modelMapper.map(changeColumn, chengeColumnInsert);
			chengeColumnInsert.setBpmItem(bpmItem);
			this.databaseService.save(chengeColumnInsert, connection);
		}

		for (BpmItemChangeColumn databaseChangeColumn : databaseChangeColumns.stream()
				.filter(a -> !item.getChangeColumns().stream().filter(b -> StringUtils.notNull(b.getId()))
						.map(b -> b.getId()).toList().contains(a.getId()))
				.toList()) {
			this.databaseService.delete(databaseChangeColumn, connection);
		}

		for (BpmItemDTO child : item.getChildren()) {
			child.setPreviousItemId(bpmItem.getId());
			if (!item.getType().equals(BpmExecuteType.TASK))
				child.setPreviousAccept(true);
			addItem(child, items, connection, bpmModel);
		}

		items.add(bpmItem);
	}

	private void checkItem(BpmItemDTO item, Connection connection) {

		List<Field> notNullFields = StaticData.classFields.get(BpmItemDTO.class).stream()
				.filter(a -> a.isAnnotationPresent(NotNull.class)).collect(Collectors.toList());

		for (Field notNullField : notNullFields)
			try {
				if (StringUtils.isNull(notNullField.get(item)))
					throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noAllRequestFieldField",
							item.getName());
			} catch (Exception e) {
				throw new WebApplicationException(e);
			}

		if (StringUtils.hasText(item.getSqlCondition()))
			UpdateColumnModel.checkDefaultValue(item.getSqlCondition());

		switch (item.getType()) {
		case EXECUTE_PROCEDURE:
			item.setAppUserId(null);
			item.setChangeColumns(new ArrayList<>());
			item.setMailBody(null);
			item.setMailSubject(null);
			item.setReceiveNotification(new ArrayList<>());
			item.setIsRejectEnd(false);

			if (StringUtils.isNull(item.getProcedure()))
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noProcedureFieldField", item.getName());

			ModelServiceImpl.checkProcedureExists(item.getProcedure());
			break;
		case SEND_NOTIFICATION:
			item.setChangeColumns(new ArrayList<>());
			item.setIsRejectEnd(false);
			item.setProcedure(null);

			if (StringUtils.isNull(item.getMailSubject()))
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noMailSubjectField", item.getName());

			if (StringUtils.isNull(item.getMailBody()))
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noMailBodyField", item.getName());

			if (item.getReceiveNotification().isEmpty())
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noReceiveNotification", item.getName());

			break;
		case TASK:
			item.setMailBody(null);
			item.setMailSubject(null);
			item.setReceiveNotification(new ArrayList<>());

			if (StringUtils.isNull(item.getAppUserId()))
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "noUserField", item.getName());

			break;

		}
	}

}
