package rs.formuvia.bpm.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import rs.formuvia.bpm.dto.BpmItemDTO;
import rs.formuvia.bpm.dto.BpmItemMailReceiveDTO;
import rs.formuvia.bpm.dto.BpmModelDTO;
import rs.formuvia.bpm.entity.BpmItem;
import rs.formuvia.bpm.entity.BpmItemPrevious;
import rs.formuvia.bpm.enums.BpmExecuteType;
import rs.formuvia.exceptions.CommonException;
import rs.formuvia.exceptions.UniqueException;
import rs.formuvia.utils.StaticData;

/**
 * The rules a process has to meet before it is saved (UpdateBpmModel), checked against a database kept in memory.
 * The front checks the same rules while the process is drawn (bpm.ts validate); the server must refuse what slips
 * past it. A procedure step that names a procedure is left out: whether the procedure exists is asked of the database.
 */
class UpdateBpmModelTest {

	private static final UUID USER = UUID.randomUUID();
	private static final UUID GROUP = UUID.randomUUID();

	@BeforeAll
	static void fieldsOfTheClasses() {
		// what the application caches on start (AppStartUpImpl) and the update reads
		for (Class<?> type : List.of(BpmItemDTO.class, BpmModelDTO.class)) {
			List<Field> fields = new ArrayList<>();
			for (Field field : type.getDeclaredFields()) {
				field.setAccessible(true);
				fields.add(field);
			}
			StaticData.classFields.put(type, fields);
		}
	}

	private static BpmItemDTO step(BpmExecuteType type, String name) {
		BpmItemDTO item = new BpmItemDTO();
		item.setName(name);
		item.setType(type);
		item.setIsAcceptEnd(false);
		item.setIsRejectEnd(false);
		return item;
	}

	/** A task one user decides, ending the process on both decisions unless told otherwise. */
	private static BpmItemDTO task(String name) {
		BpmItemDTO item = step(BpmExecuteType.TASK, name);
		item.setAppUserId(USER);
		item.setIsAcceptEnd(true);
		item.setIsRejectEnd(true);
		return item;
	}

	/** A notification to one user that ends the process. */
	private static BpmItemDTO notification(String name) {
		BpmItemDTO item = step(BpmExecuteType.SEND_NOTIFICATION, name);
		item.setMailSubject("Subject");
		item.setMailBody("Text");
		item.setReceiveNotification(new ArrayList<>(List.of(receiver())));
		item.setIsAcceptEnd(true);
		return item;
	}

	private static BpmItemMailReceiveDTO receiver() {
		BpmItemMailReceiveDTO receive = new BpmItemMailReceiveDTO();
		receive.setAppUserId(USER);
		return receive;
	}

	private static BpmItemDTO under(BpmItemDTO parent, BpmItemDTO child, Boolean accept) {
		child.setPreviousAccept(accept);
		parent.getChildren().add(child);
		return child;
	}

	private static BpmModelDTO process(BpmItemDTO... items) {
		BpmModelDTO process = new BpmModelDTO();
		process.setModelId(UUID.randomUUID());
		process.setName("Purchase");
		process.setSmtpId(UUID.randomUUID());
		process.setItems(new ArrayList<>(List.of(items)));
		return process;
	}

	private static MemoryDatabase save(BpmModelDTO process) throws Exception {
		UpdateBpmModel update = new UpdateBpmModel(process);
		MemoryDatabase database = MemoryDatabase.into(update);
		update.execute(null);
		return database;
	}

	/** The key of the message the user gets; the exception mapper looks through the causes the same way. */
	private static String refusal(BpmModelDTO process) {
		UpdateBpmModel update = new UpdateBpmModel(process);
		MemoryDatabase.into(update);
		Throwable thrown = assertThrows(Exception.class, () -> update.execute(null));
		for (Throwable cause = thrown; cause != null; cause = cause.getCause()) {
			if (cause instanceof CommonException common)
				return common.getInMessage();
			if (cause instanceof UniqueException unique)
				return unique.getMessage();
		}
		fail("refused without a message for the user: " + thrown);
		return null;
	}

	private static BpmItem saved(MemoryDatabase database, String name) {
		return database.stored(BpmItem.class).stream().filter(a -> a.getName().equals(name)).findFirst().orElseThrow();
	}

	@Test
	void aTaskThatEndsTheProcessOnBothDecisionsIsSaved() throws Exception {
		MemoryDatabase database = save(process(task("Approve")));
		assertEquals(1, database.stored(BpmItem.class).size());
		assertEquals(USER, saved(database, "Approve").getAppUser().getId());
	}

	@Test
	void aTaskIsDecidedByOneUserOrByAGroup() throws Exception {
		BpmItemDTO byGroup = task("Approve");
		byGroup.setAppUserId(null);
		byGroup.setBpmGroupId(GROUP);
		assertEquals(GROUP, saved(save(process(byGroup)), "Approve").getBpmGroup().getId());

		BpmItemDTO nobody = task("Approve");
		nobody.setAppUserId(null);
		assertEquals("noUserField", refusal(process(nobody)));
	}

	@Test
	void aTaskIsNotGivenToAUserAndAGroupAtOnce() {
		BpmItemDTO both = task("Approve");
		both.setBpmGroupId(GROUP);
		assertEquals("taskCanDoGroupOrUser", refusal(process(both)));
	}

	@Test
	void aNotificationDoesNotGoToUsersAndAGroupAtOnce() {
		BpmItemDTO both = notification("Tell");
		both.setBpmGroupId(GROUP);
		assertEquals("receiveCanDoGroupOrUser", refusal(process(both)));
	}

	@Test
	void aNotificationGoesToItsUsersOrToAGroup() throws Exception {
		BpmItemDTO toGroup = notification("Tell");
		toGroup.setReceiveNotification(new ArrayList<>());
		toGroup.setBpmGroupId(GROUP);
		assertDoesNotThrow(() -> save(process(toGroup)));

		BpmItemDTO toNobody = notification("Tell");
		toNobody.setReceiveNotification(new ArrayList<>());
		assertEquals("noReceiveNotification", refusal(process(toNobody)));
	}

	@Test
	void aNotificationNeedsASubjectAndAText() {
		BpmItemDTO noSubject = notification("Tell");
		noSubject.setMailSubject(null);
		assertEquals("noMailSubjectField", refusal(process(noSubject)));

		BpmItemDTO noText = notification("Tell");
		noText.setMailBody(null);
		assertEquals("noMailBodyField", refusal(process(noText)));
	}

	@Test
	void aProcedureStepNeedsAProcedure() {
		BpmItemDTO procedure = step(BpmExecuteType.EXECUTE_PROCEDURE, "Order");
		procedure.setIsAcceptEnd(true);
		assertEquals("noProcedureFieldField", refusal(process(procedure)));
	}

	@Test
	void everyStepNeedsItsRequiredFields() {
		BpmItemDTO noEnd = task("Approve");
		noEnd.setIsAcceptEnd(null);
		assertEquals("noAllRequestFieldField", refusal(process(noEnd)));
	}

	@Test
	void anEntryConditionMustBeASelect() {
		BpmItemDTO conditioned = task("Approve");
		conditioned.setSqlCondition("delete from purchase");
		assertEquals("onlySelectAllowed", refusal(process(conditioned)));
	}

	@Test
	void aTaskKeepsOnlyTheFieldsOfATask() throws Exception {
		BpmItemDTO approve = task("Approve");
		approve.setMailSubject("left over");
		approve.setReceiveNotification(new ArrayList<>(List.of(receiver())));
		BpmItem saved = saved(save(process(approve)), "Approve");
		assertNull(saved.getMailSubject());
	}

	@Test
	void someStepHasToEndTheProcess() {
		BpmItemDTO approve = task("Approve");
		approve.setIsAcceptEnd(false);
		BpmItemDTO tell = under(approve, notification("Tell"), true);
		tell.setIsAcceptEnd(false);
		assertEquals("noProcesFinished", refusal(process(approve)));
	}

	@Test
	void aDecisionEitherEndsTheProcessOrGoesOn() {
		BpmItemDTO acceptBoth = task("Approve");
		under(acceptBoth, notification("Tell"), true);
		assertEquals("bpmEndAndNext", refusal(process(acceptBoth)));

		BpmItemDTO rejectBoth = task("Approve");
		rejectBoth.setIsAcceptEnd(false);
		under(rejectBoth, notification("Accepted"), true);
		under(rejectBoth, notification("Rejected"), false);
		assertEquals("bpmEndAndNext", refusal(process(rejectBoth)));
	}

	@Test
	void theStepAfterAnotherKindOfStepFollowsItWhateverItSays() throws Exception {
		BpmItemDTO first = notification("First");
		first.setIsAcceptEnd(false);
		under(first, notification("Second"), false);
		BpmItemPrevious link = save(process(first)).stored(BpmItemPrevious.class).getFirst();
		assertTrue(link.getPreviousAccept());
	}

	@Test
	void aStepUnderTwoStepsIsSavedOnceWithAnArrowFromEach() throws Exception {
		BpmItemDTO boss1 = task("Boss 1");
		boss1.setIsAcceptEnd(false);
		BpmItemDTO boss2 = task("Boss 2");
		boss2.setIsAcceptEnd(false);
		for (BpmItemDTO boss : List.of(boss1, boss2)) {
			BpmItemDTO order = notification("Order");
			order.setAtLeastEnter(2);
			under(boss, order, true);
		}
		MemoryDatabase database = save(process(boss1, boss2));
		assertEquals(3, database.stored(BpmItem.class).size());
		List<BpmItemPrevious> links = database.stored(BpmItemPrevious.class);
		assertEquals(2, links.size());
		assertTrue(links.stream().allMatch(a -> a.getItem() == saved(database, "Order")));
	}

	@Test
	void aStepCannotWaitForMoreStepsThanLeadToIt() {
		BpmItemDTO boss1 = task("Boss 1");
		boss1.setIsAcceptEnd(false);
		BpmItemDTO boss2 = task("Boss 2");
		boss2.setIsAcceptEnd(false);
		for (BpmItemDTO boss : List.of(boss1, boss2)) {
			BpmItemDTO order = notification("Order");
			order.setAtLeastEnter(3);
			under(boss, order, true);
		}
		assertEquals("atLeastHigherThenNumberOfParents", refusal(process(boss1, boss2)));
	}

	@Test
	void twoProcessesOfATableCannotShareAName() {
		UpdateBpmModel update = new UpdateBpmModel(process(task("Approve")));
		MemoryDatabase.into(update).nameTaken = true;
		UniqueException exception = assertThrows(UniqueException.class, () -> update.execute(null));
		assertEquals("name", exception.getField().getName());
	}

	/* ---------------------------------------------------------------- known faults */

	@Disabled("Known fault in UpdateBpmModel: atLeastEnter below 1 is saved, so the step would never wait for anyone")
	@Test
	void aStepWaitsForAtLeastOneStep() {
		BpmItemDTO approve = task("Approve");
		approve.setAtLeastEnter(0);
		assertThrows(CommonException.class, () -> save(process(approve)));
	}

	@Disabled("Known fault in UpdateBpmModel: a step after a task without previousAccept ends in a NullPointerException "
			+ "(Boolean unboxed in the bpmEndAndNext check) instead of a message")
	@Test
	void aStepAfterATaskSaysWhichDecisionItFollows() {
		BpmItemDTO approve = task("Approve");
		approve.setIsRejectEnd(false);
		under(approve, notification("Rejected"), null);
		assertThrows(CommonException.class, () -> save(process(approve)));
	}

}
