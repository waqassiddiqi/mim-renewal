package mim.renewal.db;

import java.util.ResourceBundle;

import org.apache.log4j.Logger;

public class HistoryDatabaseConnection extends DatabaseConnection {
	private static HistoryDatabaseConnection instance = null;
	private Logger log;
	
	public HistoryDatabaseConnection() {
		log = Logger.getLogger(getClass().getName());
	}
	
	@Override
	protected void readConfig() {
		
		log.info("Initializing HistoryDatabaseConnection");
		
		ResourceBundle myResources = ResourceBundle.getBundle("renewal");
		dbUsername = myResources.getString("db.history.user");
		dbPassword = myResources.getString("db.history.password");
		dbUrl = myResources.getString("db.history.url");
		dbPort = myResources.getString("db.history.port");
		dbName = myResources.getString("db.history.name");
	}
	
	public synchronized static HistoryDatabaseConnection getInstance() {
		if(instance == null) {
			instance = new HistoryDatabaseConnection();
		}
		return instance;
	}
}