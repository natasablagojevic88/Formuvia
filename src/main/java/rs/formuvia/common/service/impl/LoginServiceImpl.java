package rs.formuvia.common.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.jvnet.hk2.annotations.Service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import jakarta.inject.Inject;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import rs.formuvia.administration.entity.AppUser;
import rs.formuvia.common.dto.LoginDTO;
import rs.formuvia.common.entity.Token;
import rs.formuvia.common.service.LoginService;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.exceptions.WrongLoginException;
import rs.formuvia.utils.CustomContainerRequestFilter;
import rs.formuvia.utils.StaticData;

@Service
public class LoginServiceImpl implements LoginService {

	@Context
	private HttpServletResponse httpServletResponse;

	@Context
	private HttpServletRequest httpServletRequest;

	@Inject
	private DatabaseService databaseService;

	public static final String ACCESS_TOKEN_NAME = "cookie.access.token.name";
	private static final String ACCESS_TOKEN_DURATION = "cookie.access.token.duration.minutes";
	private static final String REFRESH_TOKEN_DURATION = "cookie.refresh.token.duration.minutes";
	private static final String COOKIE_SECURE = "cookie.secure";
	private static final String COOKIE_SAME_SITE = "cookie.same.site";
	private static final String COOKIE_HTTP_ONLY = "cookie.http.only";
	private static final String MESSAGE_DIGEST = "SHA-256";
	private static final String SAME_SITE_ATTRIBUTE = "SameSite";
	private static final String COOKIE_PATH = "cookie.path";

	private static final String DUMMY_HASH = BCrypt.withDefaults().hashToString(12, "dummy-password".toCharArray());

	@Override
	public void login(LoginDTO loginDTO) {

		DatabaseParameter databaseParameter = DatabaseParameter.valueOf(new DatabaseFilter[] {
				DatabaseFilter.valueOf("username", loginDTO.getUsername()), DatabaseFilter.valueOf("active", "true") });

		AppUser appUser = this.databaseService.findAll(databaseParameter, AppUser.class).stream().findFirst()
				.orElse(null);

		String hash = appUser != null ? appUser.getPassword() : DUMMY_HASH;
		boolean verified = BCrypt.verifyer().verify(loginDTO.getPassword().getBytes(StandardCharsets.UTF_8),
				hash.getBytes()).verified;

		if (appUser == null || !verified) {
			throw new WrongLoginException();
		}

		Token token = new Token();
		Integer accessTokenMinutes = accessTokenMinutes();
		Integer refreshhTokenMinutes = refreshhTokenMinutes();

		LocalDateTime now = LocalDateTime.now();
		token.setAccessToken(generateToken());
		token.setAccessTokenExipires(now.plus(accessTokenMinutes, ChronoUnit.MINUTES));
		token.setRefreshToken(generateToken());
		token.setRefreshTokenExipires(now.plus(refreshhTokenMinutes, ChronoUnit.MINUTES));
		token.setAppUser(appUser);
		token = this.databaseService.save(token);

		Cookie accessTokenCookie = createCookie(StaticData.appProperties.getProperty(ACCESS_TOKEN_NAME).toString(),
				token.getAccessToken(), refreshhTokenMinutes * 60);

		this.httpServletResponse.addCookie(accessTokenCookie);

	}

	public static Integer accessTokenMinutes() {
		return Integer.valueOf(StaticData.appProperties.get(ACCESS_TOKEN_DURATION).toString());
	}

	public static Integer refreshhTokenMinutes() {
		return Integer.valueOf(StaticData.appProperties.get(REFRESH_TOKEN_DURATION).toString());
	}

	public static String generateToken() {
		try {
			byte[] bytes = new byte[96];
			new SecureRandom().nextBytes(bytes);
			String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
			String tokenHash = HexFormat.of().formatHex(
					MessageDigest.getInstance(MESSAGE_DIGEST).digest(token.getBytes(StandardCharsets.UTF_8)));
			return tokenHash;
		} catch (NoSuchAlgorithmException e) {
			throw new WebApplicationException(e);
		}
	}

	public static Cookie createCookie(String name, String value, Integer expires) {
		Cookie cookie = new Cookie(name, value);
		cookie.setMaxAge(expires);
		cookie.setSecure(Boolean.valueOf(StaticData.appProperties.getProperty(COOKIE_SECURE)));
		cookie.setHttpOnly(Boolean.valueOf(StaticData.appProperties.getProperty(COOKIE_HTTP_ONLY)));
		cookie.setAttribute(SAME_SITE_ATTRIBUTE, StaticData.appProperties.getProperty(COOKIE_SAME_SITE));
		cookie.setPath(StaticData.appProperties.getProperty(COOKIE_PATH));
		return cookie;
	}

	@Override
	public void logout() {
		if (httpServletRequest != null) {
			if (this.httpServletRequest.getAttribute(CustomContainerRequestFilter.TOKEN_ID) != null) {
				try {
					UUID uuid = (UUID) this.httpServletRequest.getAttribute(CustomContainerRequestFilter.TOKEN_ID);
					Token token = this.databaseService.findById(uuid, Token.class);
					token.setActive(false);
					this.databaseService.save(token);
				} catch (Exception e) {

				}
			}
		}
		httpServletResponse.addCookie(createCookie(StaticData.appProperties.getProperty(ACCESS_TOKEN_NAME), null, 0));

	}

}
