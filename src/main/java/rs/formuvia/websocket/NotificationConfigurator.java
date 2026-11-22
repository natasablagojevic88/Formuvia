package rs.formuvia.websocket;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import rs.formuvia.common.dto.TokenDTO;
import rs.formuvia.common.service.impl.LoginServiceImpl;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.utils.CustomContainerRequestFilter;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

public class NotificationConfigurator extends ServerEndpointConfig.Configurator {
	private final String COOKIE_HEADER = "cookie";
	private final String COOKIE_SPLIT = "\\;";
	private final String COOKIE_DELIMITER = "\\=";
	public static final String APPUSER_ID = "AppUserId";

	private DatabaseService databaseService = new DatabaseServiceImpl();

	@Override
	public boolean checkOrigin(String originHeaderValue) {
		return originHeaderValue != null && originHeaderValue
				.equals(StaticData.appProperties.getProperty(CustomContainerRequestFilter.CORS_URL));
	}

	public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {

		if (StringUtils.isNull(request.getHeaders().get(COOKIE_HEADER)))
			return;

		if (request.getHeaders().get(COOKIE_HEADER).isEmpty())
			return;

		String accessTokenCookieName = StaticData.appProperties.get(LoginServiceImpl.ACCESS_TOKEN_NAME).toString();

		String cookie = request.getHeaders().get(COOKIE_HEADER).stream()
				.flatMap(a -> Arrays.asList(a.split(COOKIE_SPLIT)).stream()).collect(Collectors.toList()).stream()
				.map(a -> a.trim().split(COOKIE_DELIMITER)).filter(a -> a.length >= 2)
				.filter(a -> a[0].equals(accessTokenCookieName)).map(a -> a[1]).findFirst().orElse(null);
		if (StringUtils.isNull(cookie))
			return;

		TokenDTO tokenDTO = this.databaseService
				.findAll(DatabaseParameter.valueOf(DatabaseFilter.valueOf("accessToken", cookie)), TokenDTO.class)
				.stream().findFirst().orElse(null);

		if (tokenDTO == null)
			return;
		if (!tokenDTO.getActive())
			return;

		if (!tokenDTO.getRefreshTokenExipires().isAfter(LocalDateTime.now()))
			return;

		sec.getUserProperties().put(APPUSER_ID, tokenDTO.getAppUserId());
	}
}
