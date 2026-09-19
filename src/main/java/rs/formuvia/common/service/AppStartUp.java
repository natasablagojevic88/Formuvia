package rs.formuvia.common.service;

import java.sql.Connection;

public interface AppStartUp {

	void initParams();

	void initConnections();

	Connection createConnection() throws Exception;

	void checkTables();

	void loadClass();
	
	void checkAdminUser();
	
	void initStaticData();
	
	void initScriptsExecute();
	
	void loadMenu();
}
