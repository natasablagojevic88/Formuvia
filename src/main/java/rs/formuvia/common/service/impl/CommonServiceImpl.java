package rs.formuvia.common.service.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import rs.formuvia.administration.entity.AppUser;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.utils.CustomContainerRequestFilter;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

@Service
public class CommonServiceImpl implements CommonService {

	Logger logger = LogManager.getLogger(getClass());
	private static final String QUERY_FOLDER = "queries";
	public static final String LOCALE_ATTRIBUTE = "locale";
	public static final String LOCALE_DEFAULT_PARAMETER = "default.language";
	private final String IP_ADDRESS_ATTRIBUTE = "ip-address";

	@Context
	private HttpServletRequest httpServletRequest;

	public CommonServiceImpl() {
	}

	public CommonServiceImpl(HttpServletRequest httpServletRequest) {
		this.httpServletRequest = httpServletRequest;
	}

	@Override
	public String readQueryFromFile(String fileName) {
		byte[] bytes = null;
		try {
			bytes = this.getClass().getClassLoader().getResourceAsStream(QUERY_FOLDER + "/" + fileName).readAllBytes();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new WebApplicationException(e);
		}
		return new String(bytes, StandardCharsets.UTF_8);
	}

	@Override
	public Locale getUserLocale() {

		if (this.httpServletRequest == null
				|| (!StringUtils.notNull(httpServletRequest.getAttribute(LOCALE_ATTRIBUTE)))) {
			return Locale.forLanguageTag(StaticData.appProperties.getProperty(LOCALE_DEFAULT_PARAMETER).toString());
		}

		return (Locale) httpServletRequest.getAttribute(LOCALE_ATTRIBUTE);
	}

	@Override
	public void setUserLocale(Locale locale) {
		if (this.httpServletRequest == null) {
			return;
		}

		httpServletRequest.setAttribute(LOCALE_ATTRIBUTE, locale);

	}

	@Override
	public AppUser getUser() {
		if (httpServletRequest == null) {
			return null;
		}

		if (this.httpServletRequest.getAttribute(CustomContainerRequestFilter.APP_USER_ATTRIBUTE) == null) {
			return null;
		}

		return (AppUser) this.httpServletRequest.getAttribute(CustomContainerRequestFilter.APP_USER_ATTRIBUTE);
	}

	@Override
	public void setIpAdress(String ipAdress) {
		if (this.httpServletRequest == null) {
			return;
		}

		this.httpServletRequest.setAttribute(IP_ADDRESS_ATTRIBUTE, ipAdress);
	}

	@Override
	public String getIpAdress() {
		if (this.httpServletRequest == null) {
			return null;
		}

		if (!StringUtils.notNull(this.httpServletRequest.getAttribute(IP_ADDRESS_ATTRIBUTE))) {
			return null;
		}

		return this.httpServletRequest.getAttribute(IP_ADDRESS_ATTRIBUTE).toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<String> getRoles() {
		if (httpServletRequest == null) {
			return new HashSet<>();
		}

		if (!StringUtils.notNull(httpServletRequest.getAttribute(CustomContainerRequestFilter.ROLE_ATTRIBUTE))) {
			return new HashSet<>();
		}

		return (Set<String>) httpServletRequest.getAttribute(CustomContainerRequestFilter.ROLE_ATTRIBUTE);
	}

}
