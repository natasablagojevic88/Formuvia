package rs.formuvia.database.utils;

import java.sql.Connection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import rs.formuvia.administration.dto.AppUserRoleDTO;
import rs.formuvia.administration.dto.RoleDTO;
import rs.formuvia.administration.entity.AppUser;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.model.dto.ModelColumnDTO;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.service.impl.ModelPreviewServiceImpl;
import rs.formuvia.model.utils.UpdateColumnModel;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

public class LoadStaticData implements ExecuteQuery<Void> {

	private DatabaseService databaseService = new DatabaseServiceImpl();

	@Override
	public Void execute(Connection connection) throws Exception {
		StaticData.appUsers = this.databaseService.findAll(null, AppUser.class, connection);
		StaticData.appUserRoles = this.databaseService.findAll(null, AppUserRoleDTO.class, connection);
		StaticData.roles = this.databaseService.findAll(null, RoleDTO.class, connection);
		StaticData.models = this.databaseService.findAll(null, ModelDTO.class, connection);
		StaticData.modelColumns = this.databaseService.findAll(null, ModelColumnDTO.class, connection);

		Set<UUID> columnsWithCodebook = columnWithCodebook();

		for (UUID modelId : columnsWithCodebook) {
			loadStaticDataCodebookModel(modelId, connection);
		}

		return null;
	}

	public static Set<UUID> columnWithCodebook() {
		return StaticData.modelColumns.stream().filter(a -> StringUtils.notNull(a.getCodebookId()))
				.map(a -> a.getCodebookId()).collect(Collectors.toSet());
	}

	public static void loadStaticDataCodebookModel(UUID modelId, Connection connection) {
		ModelDTO model = ModelPreviewServiceImpl.findModel(modelId);
		List<ModelColumnDTO> columns = findColumnsByModelIdWithDesc(modelId);
		UpdateColumnModel.loadModelStaticList(model, columns, connection);
	}

	public static List<ModelColumnDTO> findColumnsByModelId(UUID modelId) {
		List<ModelColumnDTO> columns = StaticData.modelColumns.stream().filter(a -> a.getModelId().equals(modelId))
				.sorted(Comparator.comparing(ModelColumnDTO::getRowIndex)
						.thenComparing(Comparator.comparing(ModelColumnDTO::getColumnIndex)))
				.collect(Collectors.toList());
		return columns;
	}

	public static List<ModelColumnDTO> findColumnsByModelIdWithDesc(UUID modelId) {
		List<ModelColumnDTO> columns = StaticData.modelColumns.stream().filter(a -> a.getModelId().equals(modelId))
				.filter(a -> a.getInDescriptionForCodebook())
				.sorted(Comparator.comparing(ModelColumnDTO::getRowIndex)
						.thenComparing(Comparator.comparing(ModelColumnDTO::getColumnIndex)))
				.collect(Collectors.toList());
		return columns;
	}

}
