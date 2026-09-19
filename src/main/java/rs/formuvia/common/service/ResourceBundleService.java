package rs.formuvia.common.service;

import java.util.Map;

public interface ResourceBundleService {

	String getText(String key);

	Map<String, String> getAllTexts();

}
