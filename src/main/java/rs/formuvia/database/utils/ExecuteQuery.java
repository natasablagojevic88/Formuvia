package rs.formuvia.database.utils;

import java.sql.Connection;

@FunctionalInterface
public interface ExecuteQuery<C> {

	C execute(Connection connection) throws Exception;
}
