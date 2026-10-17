package rs.formuvia.common.service;

import java.sql.Connection;
import java.util.List;

public interface FunctionListService {

	List<String> getAllFunction(String functionType, String dataType);

	List<String> getAllFunction(String functionType, String dataType, Connection connection);

}
