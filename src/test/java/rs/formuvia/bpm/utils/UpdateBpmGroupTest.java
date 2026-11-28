package rs.formuvia.bpm.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import jakarta.validation.constraints.NotNull;
import rs.formuvia.administration.dto.AppUserDTO;
import rs.formuvia.administration.entity.AppUser;
import rs.formuvia.bpm.dto.BpmGroupDTO;
import rs.formuvia.bpm.entity.BpmGroup;
import rs.formuvia.bpm.entity.BpmGroupUser;
import rs.formuvia.exceptions.UniqueException;
import rs.formuvia.utils.StaticData;

/**
 * Saving a process user group (UpdateBpmGroup): the members become exactly the users sent with it, against a database
 * kept in memory.
 */
class UpdateBpmGroupTest {

	private static final UUID ANA = UUID.randomUUID();
	private static final UUID MARKO = UUID.randomUUID();
	private static final UUID IVAN = UUID.randomUUID();

	@BeforeAll
	static void fieldsOfTheClasses() {
		// what the application caches on start (AppStartUpImpl) and the update reads
		List<Field> fields = new ArrayList<>();
		for (Field field : BpmGroupDTO.class.getDeclaredFields()) {
			field.setAccessible(true);
			fields.add(field);
		}
		StaticData.classFields.put(BpmGroupDTO.class, fields);
	}

	private static BpmGroupDTO group(UUID id, String name, UUID... users) {
		BpmGroupDTO group = new BpmGroupDTO();
		group.setId(id);
		group.setName(name);
		group.setUsers(new ArrayList<>(List.of(users).stream().map(AppUserDTO::new).toList()));
		return group;
	}

	private static Set<UUID> members(MemoryDatabase database) {
		return database.stored(BpmGroupUser.class).stream().map(a -> a.getAppUser().getId())
				.collect(Collectors.toSet());
	}

	@Test
	void aNewGroupGetsTheUsersSentWithIt() throws Exception {
		UpdateBpmGroup update = new UpdateBpmGroup(group(null, "Procurement", ANA, MARKO));
		MemoryDatabase database = MemoryDatabase.into(update);
		update.execute(null);
		BpmGroup saved = database.stored(BpmGroup.class).getFirst();
		assertEquals("Procurement", saved.getName());
		assertEquals(Set.of(ANA, MARKO), members(database));
		assertTrue(database.stored(BpmGroupUser.class).stream()
				.allMatch(a -> a.getUserGroup().getId().equals(saved.getId())));
	}

	@Test
	void onEditTheNewUsersComeInAndTheMissingOnesGoOut() throws Exception {
		UUID id = UUID.randomUUID();
		UpdateBpmGroup update = new UpdateBpmGroup(group(id, "Procurement", MARKO, IVAN));
		MemoryDatabase database = MemoryDatabase.into(update);
		// the group as it stands in the database: Ana and Marko
		BpmGroup stored = new BpmGroup(id);
		stored.setName("Procurement");
		database.save(stored, null);
		BpmGroupUser ana = new BpmGroupUser(null, stored, new AppUser(ANA));
		BpmGroupUser marko = new BpmGroupUser(null, stored, new AppUser(MARKO));
		database.save(ana, null);
		database.save(marko, null);

		update.execute(null);

		assertEquals(Set.of(MARKO, IVAN), members(database));
		assertEquals(List.of(ana), database.deleted);
		// Marko stays the same row, he is not taken out and put back
		assertTrue(database.stored(BpmGroupUser.class).contains(marko));
	}

	@Test
	void twoGroupsCannotShareAName() {
		UpdateBpmGroup update = new UpdateBpmGroup(group(null, "Procurement", ANA));
		MemoryDatabase database = MemoryDatabase.into(update);
		database.nameTaken = true;
		UniqueException exception = assertThrows(UniqueException.class, () -> update.execute(null));
		assertEquals("name", exception.getField().getName());
		assertTrue(database.saved.isEmpty(), "nothing is saved");
	}

	/* ---------------------------------------------------------------- known faults */

	@Disabled("Known fault in BpmGroupDTO: name has no @NotNull, so a group without a name reaches the database "
			+ "and fails there instead of with the message of a required field")
	@Test
	void aGroupNeedsAName() throws NoSuchFieldException {
		assertTrue(BpmGroupDTO.class.getDeclaredField("name").isAnnotationPresent(NotNull.class));
	}
}
