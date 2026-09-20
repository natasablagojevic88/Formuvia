package rs.formuvia.database.utils;

import java.sql.Connection;
import java.util.UUID;

import rs.formuvia.administration.entity.AppUser;
import rs.formuvia.database.service.DatabaseService;
import rs.formuvia.database.service.impl.DatabaseServiceImpl;
import rs.formuvia.utils.StringUtils;

public class FindAppUser implements ExecuteQuery<AppUser> {

	private final String findLoggerUser = "select nullif(current_setting('app.app_user_id', true),'')::uuid";
	private DatabaseService databaseService = new DatabaseServiceImpl();

	@Override
	public AppUser execute(Connection connection) throws Exception {
		UUID userId = databaseService.executeNativeQuery(findLoggerUser, null, UUID.class, connection).getFirst();

		if (StringUtils.isNull(userId)) {
			return null;
		}

		return databaseService.findById(userId, AppUser.class, connection);
	}

}
