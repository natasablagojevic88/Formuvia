package rs.formuvia.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;
import rs.formuvia.administration.entity.AppUser;
import rs.formuvia.common.dto.TokenDTO;
import rs.formuvia.common.entity.Token;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.impl.LoginServiceImpl;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.database.utils.DatabaseFilter;
import rs.formuvia.database.utils.DatabaseParameter;
import rs.formuvia.exceptions.ForbiddenException;
import rs.formuvia.exceptions.NotNullException;
import rs.formuvia.exceptions.UnAuthorizedException;

@Provider
public class CustomContainerRequestFilter implements ContainerRequestFilter {

	@Context
	private HttpServletRequest httpServletRequest;

	@Context
	private HttpServletResponse httpServletResponse;

	private String[] apiNonAuthorized = new String[] { "/login", "/openapi.json", "/translations" };

	private DatabaseService databaseService = new DatabaseServiceImpl();

	public static final String CONTENT_TYPE = "Content-Type";

	public static final String APP_USER_ATTRIBUTE = "AppUser";
	public static final String TOKEN_ID = "TokenID";
	public static final String ROLE_ATTRIBUTE = "Role";
	private final String LANGUAGE_HEADER = "X-Language";
	private final String IP_ADDRESS_FORWARD = "forward.ip-adress.header";

	@Context
	private ResourceInfo resource;

	private static final Jsonb JSONB = JsonbBuilder.create();

	@Inject
	private CommonService commonService;

	private final String ALLOW_CREDENDIALS_HEADER = "Access-Control-Allow-Credentials";
	private final String ALLOW_METHODS_HEADER = "Access-Control-Allow-Methods";
	private final String ALLOW_HEADERS_HEADER = "Access-Control-Allow-Headers";
	private final String ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";

	private final String CORS_URL = "cors.url";
	private final String ALLOW_CREDENTIALS = "true";
	private final String ALLOWED_HEADERS = "Content-Type, X-Language";
	private final String ALLOWED_METHODS = "GET, POST, PUT, DELETE, OPTIONS";

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		addCors();
		if ("OPTIONS".equals(requestContext.getMethod())) {
			return;
		}

		if (StringUtils.hasText(this.httpServletRequest.getHeader(LANGUAGE_HEADER))) {
			commonService.setUserLocale(Locale.forLanguageTag(this.httpServletRequest.getHeader(LANGUAGE_HEADER)));
		}

		commonService.setIpAdress(findIpAddress());

		if (!Arrays.asList(apiNonAuthorized).contains(httpServletRequest.getPathInfo())) {
			checkToken();
		}

		if ("POST".equals(requestContext.getMethod())) {
			if (requestContext.getHeaderString(CONTENT_TYPE) != null) {
				if (MediaType.APPLICATION_JSON.toLowerCase()
						.equals(requestContext.getHeaderString(CONTENT_TYPE).toLowerCase())) {
					Parameter parameter = Arrays.asList(this.resource.getResourceMethod().getParameters()).stream()
							.filter(a -> a.isAnnotationPresent(Valid.class)).findFirst().orElse(null);
					if (parameter != null) {
						byte[] inputStreamByte = requestContext.getEntityStream().readAllBytes();
						Class<?> bodyClass = (Class<?>) parameter.getParameterizedType();
						Object object = JSONB.fromJson(new String(inputStreamByte, StandardCharsets.UTF_8),
								parameter.getParameterizedType());

						List<Field> notNullField = StaticData.classFields.get(bodyClass).stream()
								.filter(a -> a.isAnnotationPresent(NotNull.class)).collect(Collectors.toList());
						for (Field field : notNullField) {
							try {
								if (StringUtils.notNull(field.get(object))) {
									continue;
								}

								throw new NotNullException(bodyClass, field.getName());
							} catch (Exception e) {
								throw new WebApplicationException(e);
							}
						}

						requestContext.setEntityStream(new ByteArrayInputStream(inputStreamByte));
					}

				}
			}
		}
	}

	private void addCors() {
		this.httpServletResponse.addHeader(ALLOW_HEADERS_HEADER, ALLOWED_HEADERS);
		this.httpServletResponse.addHeader(ALLOW_METHODS_HEADER, ALLOWED_METHODS);
		this.httpServletResponse.addHeader(ALLOW_CREDENDIALS_HEADER, ALLOW_CREDENTIALS);
		this.httpServletResponse.addHeader(ALLOW_ORIGIN_HEADER, StaticData.appProperties.getProperty(CORS_URL));
	}

	private void checkToken() {
		if (this.httpServletRequest.getCookies() == null) {
			throw new UnAuthorizedException();
		}
		List<Cookie> cookies = Arrays.asList(this.httpServletRequest.getCookies());

		String accessTokenCookieName = StaticData.appProperties.get(LoginServiceImpl.ACCESS_TOKEN_NAME).toString();
		Cookie cookie = cookies.stream().filter(a -> StringUtils.hasText(a.getName()))
				.filter(a -> a.getName().equals(accessTokenCookieName)).findFirst()
				.orElseThrow(() -> new UnAuthorizedException());

		if (!StringUtils.hasText(cookie.getValue())) {
			throw new UnAuthorizedException();
		}

		TokenDTO tokenDTO = this.databaseService
				.findAll(DatabaseParameter.valueOf(DatabaseFilter.valueOf("accessToken", cookie.getValue())),
						TokenDTO.class)
				.stream().findFirst().orElseThrow(() -> new UnAuthorizedException());

		LocalDateTime now = LocalDateTime.now();

		if (!tokenDTO.getActive()) {
			throw new UnAuthorizedException();
		}

		AppUser appUser = StaticData.appUsers.stream().filter(a -> a.getId().equals(tokenDTO.getAppUserId()))
				.findFirst().orElseThrow(() -> new UnAuthorizedException());

		if (!appUser.getActive()) {
			throw new UnAuthorizedException();
		}

		if (now.isAfter(tokenDTO.getAccessTokenExipires())) {
			if (now.isAfter(tokenDTO.getRefreshTokenExipires())) {
				throw new UnAuthorizedException();
			}

			Token token = this.databaseService.findById(tokenDTO.getId(), Token.class);
			token.setAccessToken(LoginServiceImpl.generateToken());
			token.setAccessTokenExipires(now.plus(LoginServiceImpl.accessTokenMinutes(), ChronoUnit.MINUTES));
			token.setRefreshTokenExipires(now.plus(LoginServiceImpl.refreshhTokenMinutes(), ChronoUnit.MINUTES));
			this.databaseService.save(token);

			this.httpServletResponse.addCookie(LoginServiceImpl.createCookie(accessTokenCookieName,
					token.getAccessToken(), LoginServiceImpl.refreshhTokenMinutes() * 60));
		}

		httpServletRequest.setAttribute(APP_USER_ATTRIBUTE, appUser);
		httpServletRequest.setAttribute(TOKEN_ID, tokenDTO.getId());

		List<String> roles = StaticData.appUserRoles.stream()
				.filter(a -> a.getAppUserUsername().equals(appUser.getUsername())).map(a -> a.getRoleCode())
				.collect(Collectors.toList());
		Set<String> rolesAll = new HashSet<>(roles);
		if (roles.stream().filter(a -> a.equals(RoleList.ADMIN)).count() > 0) {
			StaticData.classFields.get(RoleList.class).forEach(a -> {
				RoleList roleList = new RoleList();
				String roleCode = null;
				try {
					roleCode = a.get(roleList).toString();
				} catch (Exception e) {
					throw new WebApplicationException(e);
				}
				rolesAll.add(roleCode);
			});

		}
		httpServletRequest.setAttribute(ROLE_ATTRIBUTE, rolesAll);

		if (this.resource.getResourceMethod().isAnnotationPresent(RolesAllowed.class)) {
			RolesAllowed rolesAllowed = this.resource.getResourceMethod().getAnnotation(RolesAllowed.class);
			if (rolesAllowed.value().length > 0) {

				Boolean hasRole = Arrays.asList(rolesAllowed.value()).stream().filter(a -> rolesAll.contains(a))
						.count() > 0;
				if (!hasRole) {
					throw new ForbiddenException();
				}
			}
		}

	}

	private String findIpAddress() {
		if (!StringUtils.hasText(StaticData.appProperties.getProperty(IP_ADDRESS_FORWARD))) {
			return this.httpServletRequest.getRemoteAddr();
		} else {
			String forwardAddress = StaticData.appProperties.getProperty(IP_ADDRESS_FORWARD);

			if (StringUtils.hasText(this.httpServletRequest.getHeader(forwardAddress))) {
				return this.httpServletRequest.getHeader(forwardAddress);
			} else {
				return this.httpServletRequest.getRemoteAddr();
			}
		}
	}

}
