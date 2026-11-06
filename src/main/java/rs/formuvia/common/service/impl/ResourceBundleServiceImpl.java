package rs.formuvia.common.service.impl;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.ResourceBundleService;
import rs.formuvia.utils.StaticData;
import rs.formuvia.utils.StringUtils;

@Service
public class ResourceBundleServiceImpl implements ResourceBundleService {

	@Context
	private HttpServletRequest httpServletRequest;

	private static final String CUSTOM_RESOURCE_BUNDLE_PATH_PROP = "resource.bundle.path";
	public static final String CUSTOM_RESOURCE_FILE_PROP = "resource.bundle.file.prefix";

	private static Logger logger = LogManager.getLogger(ResourceBundleServiceImpl.class);

	public ResourceBundleServiceImpl() {
	}

	public ResourceBundleServiceImpl(HttpServletRequest httpServletRequest) {
		this.httpServletRequest = httpServletRequest;
	}

	private final String[] RESOURCE_BUNDLE_PATH = new String[] { "error", "dto", "menu", "common", "enum" };
	private final String RESOURCE_BUNDLE_PREFIX = "resource_bundle_";

	private static volatile URLClassLoader custoUrlLoader;

	@Inject
	private CommonService commonService;

	@Override
	public String getText(String key) {
		if (StringUtils.isNull(key)) {
			return "";
		}

		commonService = commonService == null ? new CommonServiceImpl(httpServletRequest) : commonService;

		ResourceBundle resourceBundle = null;

		Locale locale = commonService.getUserLocale();

		if (custoUrlLoader != null) {
			try {
				ResourceBundle customResourceBundle = ResourceBundle.getBundle(
						StaticData.appProperties.get(CUSTOM_RESOURCE_FILE_PROP).toString(), locale, custoUrlLoader);
				if (customResourceBundle.containsKey(key))
					return customResourceBundle.getString(key);
			} catch (Exception e) {
			}
		}

		for (String path : RESOURCE_BUNDLE_PATH) {
			resourceBundle = ResourceBundle.getBundle(RESOURCE_BUNDLE_PREFIX + path, locale);
			if (resourceBundle.containsKey(key)) {
				return resourceBundle.getString(key);
			}
		}

		return key;
	}

	public static URLClassLoader customURLClassLoader() {
		if (StringUtils.notNull(StaticData.appProperties.get(CUSTOM_RESOURCE_BUNDLE_PATH_PROP))
				&& StringUtils.notNull(StaticData.appProperties.get(CUSTOM_RESOURCE_FILE_PROP))) {
			File file = new File(StaticData.appProperties.get(CUSTOM_RESOURCE_BUNDLE_PATH_PROP).toString());

			if (file.exists()) {
				if (file.isDirectory()) {
					try {
						custoUrlLoader = new URLClassLoader(new URL[] { file.toURI().toURL() });
						return custoUrlLoader;
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}

				} else {
					logger.error(StaticData.appProperties.get(CUSTOM_RESOURCE_BUNDLE_PATH_PROP).toString()
							+ " is not directory");
				}
			} else {
				logger.error("No folder exists: "
						+ StaticData.appProperties.get(CUSTOM_RESOURCE_BUNDLE_PATH_PROP).toString());
			}

		}

		return null;
	}
}
