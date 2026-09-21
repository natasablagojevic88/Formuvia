package rs.formuvia.model.utils;

import java.net.HttpURLConnection;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.modelmapper.ModelMapper;

import lombok.RequiredArgsConstructor;
import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.database.enums.SearchOperation;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.service.impl.SqlQueryWriterServiceImpl;
import rs.formuvia.database.utils.CheckTables;
import rs.formuvia.database.utils.ColumnInfo;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.database.utils.ForeignKeyInfo;
import rs.formuvia.database.utils.IndexInfo;
import rs.formuvia.database.utils.TableInfo;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.exceptions.NotNullException;
import rs.formuvia.exceptions.UniqueException;
import rs.formuvia.model.dto.ModelDTO;
import rs.formuvia.model.entity.Model;
import rs.formuvia.model.enums.ModelType;
import rs.formuvia.utils.StringUtils;

@RequiredArgsConstructor
public class CreateModel implements ExecuteQuery<ModelDTO> {
	private final ModelDTO modelDTO;

	private DatabaseService databaseService = new DatabaseServiceImpl();
	private ModelMapper modelMapper = new ModelMapper();

	private final String PARENT_COLUMN_NAME = "parent";
	private final String PARENT_FOREIGN_KEY_NAME = "fk_#model_name#_parent";
	private final String MODEL_NAME_REPLACE = "#model_name#";
	private final String PARENT_INDEX_NAME = "#model_name#_index_parent";
	private static final Pattern NAME_PARENT = Pattern.compile("^[a-z][a-z0-9_]{0,62}$");

	@Override
	public ModelDTO execute(Connection connection) throws Exception {
		Model model = modelDTO.getId() == null ? new Model()
				: this.databaseService.findById(modelDTO.getId(), Model.class, connection);

		if (modelDTO.getId() != null && modelDTO.getType().equals(ModelType.TABLE)) {
			if (!model.getCode().equals(modelDTO.getCode())) {
				modelDTO.setCode(model.getCode());
			}
		}

		checkModel(modelDTO, connection);

		modelMapper.map(modelDTO, model);
		
		if(modelDTO.getParentId()!=null) 
			model.setParent(this.databaseService.findById(modelDTO.getParentId(), Model.class, connection));
		else
			model.setParent(null);

		model = this.databaseService.save(model, connection);

		if (model.getType().equals(ModelType.TABLE) && StringUtils.isNull(modelDTO.getId())) {
			createTable(modelDTO, connection);
		}

		return modelMapper.map(model, ModelDTO.class);
	}

	private void createTable(ModelDTO modelDTO, Connection connection) {
		List<String> tables = CheckTables.allTable(connection);

		if (tables.stream().filter(a -> a.equals(modelDTO.getCode())).count() > 0) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "tableAlreadyExists", null);
		}
		TableInfo tableInfo = new TableInfo();
		tableInfo.setName(modelDTO.getCode());

		ColumnInfo idColumn = new ColumnInfo();
		idColumn.setColumnType(ColumnType.UUID);
		idColumn.setIsPrimary(true);
		idColumn.setNullable(false);
		idColumn.setTableName(modelDTO.getCode());
		idColumn.setName(SqlQueryWriterServiceImpl.defaultIdColumn);
		tableInfo.getColumns().add(idColumn);

		Model parentModel = this.databaseService.findById(modelDTO.getParentId(), Model.class, connection);

		if (parentModel.getType().equals(ModelType.TABLE)) {
			ColumnInfo parentColumn = new ColumnInfo();
			parentColumn.setColumnType(ColumnType.UUID);
			parentColumn.setIsPrimary(false);
			parentColumn.setName(PARENT_COLUMN_NAME);
			parentColumn.setNullable(false);
			parentColumn.setTableName(modelDTO.getCode());
			tableInfo.getColumns().add(parentColumn);

			ForeignKeyInfo foreignKeyInfo = new ForeignKeyInfo();
			foreignKeyInfo.setCascadeDelete(true);
			foreignKeyInfo.setColumnName(PARENT_COLUMN_NAME);
			foreignKeyInfo.setName(PARENT_FOREIGN_KEY_NAME.replaceAll(MODEL_NAME_REPLACE, modelDTO.getCode()));
			foreignKeyInfo.setReferenceTable(parentModel.getCode());
			foreignKeyInfo.setTableName(modelDTO.getCode());
			tableInfo.getForeignKeys().add(foreignKeyInfo);

			IndexInfo indexInfo = new IndexInfo();
			indexInfo.setColumnName(PARENT_COLUMN_NAME);
			indexInfo.setName(PARENT_INDEX_NAME.replaceAll(MODEL_NAME_REPLACE, modelDTO.getCode()));
			indexInfo.setTableName(modelDTO.getCode());
			tableInfo.getIndexes().add(indexInfo);
		}

		CheckTables createTable = new CheckTables(Arrays.asList(tableInfo));
		this.databaseService.executeQuery(createTable, connection);
	}

	private void checkModel(ModelDTO modelDTO, Connection connection) {

		if (modelDTO.getId() == null) {
			if (modelDTO.getType().equals(ModelType.TABLE)) {
				if (this.databaseService.exists(
						DatabaseParameter.valueOf(DatabaseFilter.valueOf("code", modelDTO.getCode())), Model.class,
						connection)) {
					throw new UniqueException(UniqueException.findFieldFromList(Model.class, "code"),
							modelDTO.getCode());
				}
			}

			if (this.databaseService.exists(
					DatabaseParameter.valueOf(DatabaseFilter.valueOf("name", modelDTO.getName())), Model.class,
					connection)) {
				throw new UniqueException(UniqueException.findFieldFromList(Model.class, "name"), modelDTO.getName());
			}

		} else {
			if (modelDTO.getType().equals(ModelType.TABLE)) {
				if (this.databaseService.exists(
						DatabaseParameter.valueOf(new DatabaseFilter[] {
								DatabaseFilter.valueOf("code", modelDTO.getCode()), DatabaseFilter.valueOf("id",
										SearchOperation.NOT_EQUALS, modelDTO.getId().toString()) }),
						Model.class, connection)) {
					throw new UniqueException(UniqueException.findFieldFromList(Model.class, "code"),
							modelDTO.getCode());
				}
			}

			if (this.databaseService.exists(
					DatabaseParameter.valueOf(new DatabaseFilter[] { DatabaseFilter.valueOf("name", modelDTO.getName()),
							DatabaseFilter.valueOf("id", SearchOperation.NOT_EQUALS, modelDTO.getId().toString()) }),
					Model.class, connection)) {
				throw new UniqueException(UniqueException.findFieldFromList(Model.class, "name"), modelDTO.getName());
			}
		}

		if (modelDTO.getType().equals(ModelType.MENU) && StringUtils.notNull(modelDTO.getParentId())) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "menuDoesNotHaveParent", null);
		}

		if (modelDTO.getType().equals(ModelType.MENU)) {
			modelDTO.setCode(null);
			modelDTO.setAddRoleId(null);
			modelDTO.setPreviewRoleId(null);
			modelDTO.setUpdateRoleId(null);
			modelDTO.setDeleteRoleId(null);
		}

		if (modelDTO.getCode() != null) {
			if (!NAME_PARENT.matcher(modelDTO.getCode()).matches()) {
				throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "invalidTableName", modelDTO.getCode());
			}

		}

		if (modelDTO.getType().equals(ModelType.TABLE) && StringUtils.isNull(modelDTO.getParentId())) {
			throw new CommonException(HttpURLConnection.HTTP_BAD_REQUEST, "tableMustHaveParent", null);
		}

		if (modelDTO.getType().equals(ModelType.TABLE) && (!StringUtils.hasText(modelDTO.getCode()))) {
			throw new NotNullException(UniqueException.findFieldFromList(ModelDTO.class, "code"));
		}

		if (modelDTO.getType().equals(ModelType.TABLE) && StringUtils.isNull(modelDTO.getPreviewRoleId())) {
			throw new NotNullException(UniqueException.findFieldFromList(ModelDTO.class, "previewRoleId"));
		}

		if (modelDTO.getType().equals(ModelType.TABLE) && StringUtils.isNull(modelDTO.getAddRoleId())) {
			throw new NotNullException(UniqueException.findFieldFromList(ModelDTO.class, "addRoleId"));
		}

		if (modelDTO.getType().equals(ModelType.TABLE) && StringUtils.isNull(modelDTO.getUpdateRoleId())) {
			throw new NotNullException(UniqueException.findFieldFromList(ModelDTO.class, "updateRoleId"));
		}

		if (modelDTO.getType().equals(ModelType.TABLE) && StringUtils.isNull(modelDTO.getDeleteRoleId())) {
			throw new NotNullException(UniqueException.findFieldFromList(ModelDTO.class, "deleteRoleId"));
		}
	}

}
