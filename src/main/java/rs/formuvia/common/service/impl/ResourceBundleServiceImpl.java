package rs.formuvia.common.service.impl;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.ResourceBundleService;

@Service
public class ResourceBundleServiceImpl implements ResourceBundleService {

	@Context
	private HttpServletRequest httpServletRequest;

	public ResourceBundleServiceImpl() {
	}

	public ResourceBundleServiceImpl(HttpServletRequest httpServletRequest) {
		this.httpServletRequest = httpServletRequest;
	}

	private final String[] RESOURCE_BUNDLE_PATH = new String[] { "error", "dto", "menu", "common" };
	private final String RESOURCE_BUNDLE_PREFIX = "resource_bundle_";

	@Inject
	private CommonService commonService;

	@Override
	public String getText(String key) {
		commonService = commonService == null ? new CommonServiceImpl(httpServletRequest) : commonService;

		ResourceBundle resourceBundle = null;

		Locale locale = commonService.getUserLocale();

		for (String path : RESOURCE_BUNDLE_PATH) {
			resourceBundle = ResourceBundle.getBundle(RESOURCE_BUNDLE_PREFIX + path, locale);
			if (resourceBundle.containsKey(key)) {
				return resourceBundle.getString(key);
			}
		}

		return key;
	}
}
