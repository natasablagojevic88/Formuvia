package rs.formuvia.database.service.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import rs.formuvia.administration.entity.AppUser;
import rs.formuvia.common.service.AppStartUp;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.impl.AppStartUpImpl;
import rs.formuvia.common.service.impl.CommonServiceImpl;
import rs.formuvia.database.enums.ColumnType;
import rs.formuvia.database.enums.SearchOperation;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.utils.CreateDatabaseTable;
import rs.formuvia.database.utils.CreateDelete;
import rs.formuvia.database.utils.CreateExists;
import rs.formuvia.database.utils.CreateSave;
import rs.formuvia.database.utils.CreateTable;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.database.utils.ExecuteNativeQueryImpl;
import rs.formuvia.database.utils.ExecuteQuery;
import rs.formuvia.database.utils.ExecuteUpdate;
import rs.formuvia.database.utils.FindAppUser;
import rs.formuvia.database.utils.GenerateQueryFromDTO;
import rs.formuvia.exceptions.NoDataFoundException;
import rs.formuvia.utils.StaticData;

@Service
public class DatabaseServiceImpl implements DatabaseService {

	@Context
	private HttpServletRequest httpServletRequest;

	@Inject
	private CommonService commonService;

	Logger logger = LogManager.getLogger(getClass());
	public static final String CHECK_QUERY = "database.check.query";
	private final String RANDOM_UUID_QUERY = "SELECT " + CreateTable.ID_DEFAULT;
	private final String SET_USER_QUERY = "select set_config('app.app_user_id', ?, true)";
	private final String SET_IP_ADDRESS_QUERY = "select set_config('app.ip_address', ?, true)";
	private final Integer SECONDS_TO_WAIT = 5;

	private <C> C readQuery(ExecuteQuery<C> executeQuery) {
		boolean commit = true;
		Exception outError = null;
		C outObject = null;
		Connection connection = null;
		try {

			try {
				connection = StaticData.connections.poll(SECONDS_TO_WAIT, TimeUnit.SECONDS);
				String checkConnectionQuery = StaticData.appProperties.getProperty(CHECK_QUERY);
				PreparedStatement preparedStatement = connection.prepareStatement(checkConnectionQuery);
				preparedStatement.executeQuery();
				preparedStatement.close();
			} catch (Exception e) {
				try {
					connection.close();
				} catch (Exception ignore) {
				}
				Integer MAXIMUM_CONNECTION_NUMBER = Integer
						.valueOf(StaticData.appProperties.get(AppStartUpImpl.CONNECTION_NUMBER).toString());
				if (StaticData.allConnections.contains(connection)) {
					StaticData.allConnections.remove(connection);
				}

				if (StaticData.allConnections.size() < MAXIMUM_CONNECTION_NUMBER) {
					AppStartUp appStartUp = new AppStartUpImpl();
					connection = appStartUp.createConnection();
				} else {
					throw new WebApplicationException("NO_FREE_CONNECTION");
				}

			}

			commonService = commonService == null ? new CommonServiceImpl(httpServletRequest) : commonService;

			AppUser appUser = commonService.getUser();
			String userId = appUser == null ? null : appUser.getId().toString();

			PreparedStatement preparedStatement = connection.prepareStatement(SET_USER_QUERY);
			preparedStatement.setObject(1, userId);
			preparedStatement.execute();
			preparedStatement.close();

			preparedStatement = connection.prepareStatement(SET_IP_ADDRESS_QUERY);
			preparedStatement.setObject(1, commonService.getIpAdress());
			preparedStatement.execute();
			preparedStatement.close();

			outObject = executeQuery.execute(connection);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			commit = false;
			outError = e;
		} finally {
			if (connection != null) {
				try {
					if (commit)
						connection.commit();
					else
						connection.rollback();

				} catch (SQLException e) {
					logger.error(e.getMessage(), e);
					try {
						connection.rollback();
					} catch (Exception ignore) {
					}
					throw new WebApplicationException(e);
				} finally {
					StaticData.connections.offer(connection);
				}
			}

		}
		if (outError != null)
			throw new WebApplicationException(outError);
		else
			return outObject;
	}

	private <C> C readQueryWithConnection(ExecuteQuery<C> executeQuery, Connection connection) {
		try {
			return executeQuery.execute(connection);
		} catch (Exception e) {
			this.logger.error(e.getMessage(), e);
			throw new WebApplicationException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <C> List<C> executeNativeQuery(String query, Map<Integer, Object> parameters, Class<C> resultClass) {
		ExecuteQuery<C> executeQuery = new ExecuteNativeQueryImpl<>(query, parameters, resultClass);
		return (List<C>) readQuery(executeQuery);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <C> List<C> executeNativeQuery(String query, Map<Integer, Object> parameters, Class<C> resultClass,
			Connection connection) {
		ExecuteQuery<C> executeQuery = new ExecuteNativeQueryImpl<>(query, parameters, resultClass);
		return (List<C>) readQueryWithConnection(executeQuery, connection);
	}

	@Override
	public void executeUpdateQuery(String query, Map<Integer, Object> parameters) {
		readQuery(new ExecuteUpdate(query, parameters));

	}

	@Override
	public void executeUpdateQuery(String query, Map<Integer, Object> parameters, Connection connection) {
		readQueryWithConnection(new ExecuteUpdate(query, parameters), connection);

	}

	@Override
	public <C> C executeQuery(ExecuteQuery<C> executeQuery) {

		return readQuery(executeQuery);
	}

	@Override
	public <C> C executeQuery(ExecuteQuery<C> executeQuery, Connection connection) {

		return readQueryWithConnection(executeQuery, connection);
	}

	@Override
	public UUID generateUUID() {

		return executeNativeQuery(RANDOM_UUID_QUERY, null, UUID.class).getFirst();
	}

	@Override
	public UUID generateUUID(Connection connection) {
		return executeNativeQuery(RANDOM_UUID_QUERY, null, UUID.class, connection).getFirst();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <C> List<C> findAll(DatabaseParameter databaseParameter, Class<C> resultClass) {
		GenerateQueryFromDTO<C> generateQueryFromDTO = new GenerateQueryFromDTO<>(databaseParameter, resultClass);
		return (List<C>) readQuery(generateQueryFromDTO);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <C> List<C> findAll(DatabaseParameter databaseParameter, Class<C> resultClass, Connection connection) {
		GenerateQueryFromDTO<C> generateQueryFromDTO = new GenerateQueryFromDTO<>(databaseParameter, resultClass);
		return (List<C>) readQueryWithConnection(generateQueryFromDTO, connection);
	}

	@Override
	public <C> C findById(UUID id, Class<C> resultClass) {
		DatabaseParameter databaseParameter = createFindByIdParameters(id);
		List<C> list = findAll(databaseParameter, resultClass);
		if (list.isEmpty()) {
			throw new NoDataFoundException(id, resultClass);
		} else {
			return list.getFirst();
		}
	}

	@Override
	public <C> C findById(UUID id, Class<C> resultClass, Connection connection) {
		DatabaseParameter databaseParameter = createFindByIdParameters(id);
		List<C> list = findAll(databaseParameter, resultClass, connection);
		if (list.isEmpty()) {
			NoDataFoundException dataFoundException = new NoDataFoundException(id, resultClass);
			this.logger.error(dataFoundException.getMessage() + ":" + id + ":" + resultClass.getSimpleName());
			throw dataFoundException;
		} else {
			return list.getFirst();
		}
	}

	public static DatabaseParameter createFindByIdParameters(UUID id) {
		DatabaseParameter databaseParameter = new DatabaseParameter();
		databaseParameter.setPageSize(1);
		databaseParameter.getFilters().add(new DatabaseFilter(SqlQueryWriterServiceImpl.defaultIdColumn,
				SearchOperation.EQUALS, ColumnType.UUID, id.toString(), null));
		return databaseParameter;
	}

	@Override
	public <C> DatabaseTable<C> createTable(DatabaseParameter databaseParameter, Class<C> resultClass) {
		CreateDatabaseTable<C> createDatabaseTable = new CreateDatabaseTable<>(databaseParameter, resultClass,
				httpServletRequest);
		return readQuery(createDatabaseTable);
	}

	@Override
	public <C> DatabaseTable<C> createTable(DatabaseParameter databaseParameter, Class<C> resultClass,
			Connection connection) {
		CreateDatabaseTable<C> createDatabaseTable = new CreateDatabaseTable<>(databaseParameter, resultClass,
				httpServletRequest);
		return readQueryWithConnection(createDatabaseTable, connection);
	}

	@Override
	public <C> C save(C entity) {
		CreateSave<C> createSave = new CreateSave<C>(entity);
		return readQuery(createSave);
	}

	@Override
	public <C> C save(C entity, Connection connection) {
		CreateSave<C> createSave = new CreateSave<C>(entity);
		return readQueryWithConnection(createSave, connection);
	}

	@Override
	public <C> void delete(C entity) {
		CreateDelete<C> createDelete = new CreateDelete<>(entity);
		readQuery(createDelete);
	}

	@Override
	public <C> void delete(C entity, Connection connection) {
		CreateDelete<C> createDelete = new CreateDelete<>(entity);
		readQueryWithConnection(createDelete, connection);

	}

	@Override
	public <C> Boolean exists(DatabaseParameter databaseParameter, Class<C> resultClass) {
		CreateExists<C> createExists = new CreateExists<>(databaseParameter, resultClass);
		return readQuery(createExists);
	}

	@Override
	public <C> Boolean exists(DatabaseParameter databaseParameter, Class<C> resultClass, Connection connection) {
		CreateExists<C> createExists = new CreateExists<>(databaseParameter, resultClass);
		return readQueryWithConnection(createExists, connection);
	}

	@Override
	public AppUser getUser(Connection connection) {
		FindAppUser findUser = new FindAppUser();
		return readQueryWithConnection(findUser, connection);
	}
}
