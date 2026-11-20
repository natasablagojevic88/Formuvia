package rs.formuvia.websocket;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.websocket.CloseReason;
import jakarta.websocket.CloseReason.CloseCodes;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import rs.formuvia.utils.ApiRoute;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

@ServerEndpoint(value = "/api" + ApiRoute.websocketNotification, configurator = NotificationConfigurator.class)
public class NotificationWebSocket {
	private final String CLOSE_REASON = "Unauthorized";

	private static Logger logger = LogManager.getLogger(NotificationWebSocket.class);

	@OnOpen
	public void onOpen(Session session, @PathParam("id") String id) throws IOException {
		if (StringUtils.isNull(session.getUserProperties().get(NotificationConfigurator.APPUSER_ID))) {
			session.close(new CloseReason(CloseCodes.VIOLATED_POLICY, CLOSE_REASON));
			return;
		}

		if (!session.getUserProperties().get(NotificationConfigurator.APPUSER_ID).toString().equals(id)) {
			session.close(new CloseReason(CloseCodes.VIOLATED_POLICY, CLOSE_REASON));
			return;
		}
		UUID uuid = UUID.fromString(id);
		if (StaticData.websocketSession.get(uuid) == null) {
			Set<Session> set = ConcurrentHashMap.newKeySet();
			set.add(session);
			StaticData.websocketSession.put(uuid, set);
		}

		else
			StaticData.websocketSession.get(uuid).add(session);

	}

	@OnClose
	public void onClose(Session session) {
		if (StringUtils.isNull(session.getUserProperties().get(NotificationConfigurator.APPUSER_ID))) {
			return;
		}

		UUID id = (UUID) session.getUserProperties().get(NotificationConfigurator.APPUSER_ID);

		if (StaticData.websocketSession.get(id) == null)
			return;

		if (!StaticData.websocketSession.get(id).contains(session))
			return;

		StaticData.websocketSession.computeIfPresent(id, (_, sesions) -> {
			sesions.remove(session);
			return sesions;
		});
	}

	@OnError
	public void onError(Session session, Throwable throwable) {
		logger.error(throwable);
	}

	public static void sendMessage(Session session, String text) {
		if (!session.isOpen())
			return;

		try {
			session.getBasicRemote().sendText(text);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}
}
