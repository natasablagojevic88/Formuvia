package rs.formuvia.common.service.impl;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Inject;
import rs.formuvia.common.service.CommonService;
import rs.formuvia.common.service.FunctionListService;
import rs.formuvia.database.service.DatabaseService;

@Service
public class FunctionListServiceImpl implements FunctionListService {

	@Inject
	private CommonService commonService;

	@Inject
	private DatabaseService databaseService;

	private final String ALL_FUNCTION_QUERY_FILE = "allFunctions.sql";

	@Override
	public List<String> getAllFunction(String functionType, String dataType) {
		String query = commonService.readQueryFromFile(ALL_FUNCTION_QUERY_FILE);

		Map<Integer, Object> parameters = new HashMap<>();
		parameters.put(1, functionType);
		parameters.put(2, dataType == null ? "%" : dataType);
		List<Object[]> result = this.databaseService.executeNativeQuery(query, parameters, Object[].class);

		return result.stream().map(a -> a[0].toString()).sorted(Comparator.comparing(a -> a)).toList();
	}

}
