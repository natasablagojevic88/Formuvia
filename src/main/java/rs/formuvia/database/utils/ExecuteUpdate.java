package rs.formuvia.database.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Iterator;
import java.util.Map;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ExecuteUpdate implements ExecuteQuery<Void> {

	private final String query;
	private final Map<Integer, Object> parameters;

	@Override
	public Void execute(Connection connection) throws Exception {
		PreparedStatement preparedStatement = connection.prepareStatement(query);
		if (parameters != null) {
			Iterator<Integer> iterator = parameters.keySet().iterator();
			while (iterator.hasNext()) {
				Integer key = iterator.next();
				preparedStatement.setObject(key, parameters.get(key));
			}
		}

		preparedStatement.executeUpdate();
		preparedStatement.close();
		return null;
	}

}
