package rs.formuvia.bpm.utils;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import rs.formuvia.administration.entity.AppUser;
import rs.formuvia.bpm.dto.BpmGroupUserDTO;
import rs.formuvia.bpm.dto.BpmItemPreviousDTO;
import rs.formuvia.bpm.entity.BpmGroupUser;
import rs.formuvia.bpm.entity.BpmItemPrevious;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.database.utils.DatabaseTable;
import rs.formuvia.database.utils.ExecuteQuery;

/**
 * Stands in for the database in the tests of the processes and their groups: what is saved stays in memory, and only
 * the reads those classes make are answered. Anything else fails, so a new read is noticed instead of passing silently.
 */
class MemoryDatabase implements DatabaseService {

	final List<Object> saved = new ArrayList<>();
	final List<Object> deleted = new ArrayList<>();
	/** true: another process of the model already has the name. */
	boolean nameTaken;

	/** Puts this database in place of the one the class under test made for itself. */
	static MemoryDatabase into(Object target) {
		MemoryDatabase database = new MemoryDatabase();
		try {
			Field field = target.getClass().getDeclaredField("databaseService");
			field.setAccessible(true);
			field.set(target, database);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("the class has no databaseService field any more", e);
		}
		return database;
	}

	<C> List<C> stored(Class<C> type) {
		return saved.stream().filter(type::isInstance).map(type::cast).filter(a -> !deleted.contains(a)).toList();
	}

	private static String filter(DatabaseParameter parameter, String field) {
		return parameter.getFilters().stream().filter(a -> a.getField().equals(field)).map(DatabaseFilter::getField1)
				.findFirst().orElse(null);
	}

	private List<BpmItemPrevious> links(DatabaseParameter parameter) {
		String item = filter(parameter, "item");
		String parent = filter(parameter, "parent");
		return stored(BpmItemPrevious.class).stream()
				.filter(a -> item == null || a.getItem().getId().toString().equals(item))
				.filter(a -> parent == null || a.getParent().getId().toString().equals(parent)).toList();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <C> List<C> findAll(DatabaseParameter databaseParameter, Class<C> resultClass, Connection connection) {
		if (resultClass == BpmItemPreviousDTO.class)
			return (List<C>) stored(BpmItemPrevious.class).stream()
					.map(a -> new BpmItemPreviousDTO(a.getId(), a.getItem().getId(), null, a.getParent().getId(),
							a.getPreviousAccept()))
					.toList();
		if (resultClass == BpmItemPrevious.class)
			return (List<C>) links(databaseParameter);
		if (resultClass == BpmGroupUser.class) {
			String group = filter(databaseParameter, "userGroup");
			return (List<C>) stored(BpmGroupUser.class).stream()
					.filter(a -> a.getUserGroup().getId().toString().equals(group)).toList();
		}
		if (resultClass == BpmGroupUserDTO.class)
			throw new UnsupportedOperationException("read through BpmGroupItem, which the tests leave out");
		// the steps, receivers and columns already in the database: a new process has none
		return new ArrayList<>();
	}

	@Override
	public <C> Boolean exists(DatabaseParameter databaseParameter, Class<C> resultClass, Connection connection) {
		if (resultClass == BpmItemPrevious.class)
			return !links(databaseParameter).isEmpty();
		return nameTaken;
	}

	@Override
	public <C> C save(C entity, Connection connection) {
		try {
			Field id = entity.getClass().getDeclaredField("id");
			id.setAccessible(true);
			if (id.get(entity) == null)
				id.set(entity, UUID.randomUUID());
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
		if (saved.stream().noneMatch(a -> a == entity))
			saved.add(entity);
		return entity;
	}

	@Override
	public <C> void delete(C entity, Connection connection) {
		deleted.add(entity);
	}

	@Override
	public <C> C findById(UUID id, Class<C> resultClass, Connection connection) {
		return saved.stream().filter(resultClass::isInstance).map(resultClass::cast).filter(a -> {
			try {
				Field field = a.getClass().getDeclaredField("id");
				field.setAccessible(true);
				return Objects.equals(field.get(a), id);
			} catch (ReflectiveOperationException e) {
				return false;
			}
		}).findFirst().orElseThrow();
	}

	/** The process or the group read back at the end: the tests look at what was saved instead. */
	@Override
	public <C> C executeQuery(ExecuteQuery<C> executeQuery, Connection connection) {
		return null;
	}

	@Override
	public <C> List<C> executeNativeQuery(String query, Map<Integer, Object> parameters, Class<C> resultClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <C> List<C> executeNativeQuery(String query, Map<Integer, Object> parameters, Class<C> resultClass,
			Connection connection) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void executeUpdateQuery(String query, Map<Integer, Object> parameters) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void executeUpdateQuery(String query, Map<Integer, Object> parameters, Connection connection) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <C> C executeQuery(ExecuteQuery<C> executeQuery) {
		throw new UnsupportedOperationException();
	}

	@Override
	public UUID generateUUID() {
		throw new UnsupportedOperationException();
	}

	@Override
	public UUID generateUUID(Connection connection) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <C> List<C> findAll(DatabaseParameter databaseParameter, Class<C> resultClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <C> C findById(UUID id, Class<C> resultClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <C> DatabaseTable<C> createTable(DatabaseParameter databaseParameter, Class<C> resultClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <C> DatabaseTable<C> createTable(DatabaseParameter databaseParameter, Class<C> resultClass,
			Connection connection) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <C> C save(C entity) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <C> void delete(C entity) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <C> Boolean exists(DatabaseParameter databaseParameter, Class<C> resultClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public AppUser getUser(Connection connection) {
		throw new UnsupportedOperationException();
	}
}
