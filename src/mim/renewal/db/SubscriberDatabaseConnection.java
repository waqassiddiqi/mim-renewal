package mim.renewal.db;

import java.util.ResourceBundle;

import org.apache.log4j.Logger;

public class SubscriberDatabaseConnection extends DatabaseConnection {
	private static SubscriberDatabaseConnection instance = null;
	private Logger log;
	
	public SubscriberDatabaseConnection() {
		log = Logger.getLogger(getClass().getName());
	}
	
	@Override
	protected void readConfig() {
		
		log.info("Initializing SubscriberDatabaseConnection");
		
		ResourceBundle myResources = ResourceBundle.getBundle("renewal");
		dbUsername = myResources.getString("db.mim.user");
		dbPassword = myResources.getString("db.mim.password");
		dbUrl = myResources.getString("db.mim.url");
		dbPort = myResources.getString("db.mim.port");
		dbName = myResources.getString("db.mim.name");
	}
	
	public synchronized static SubscriberDatabaseConnection getInstance() {
		if(instance == null) {
			instance = new SubscriberDatabaseConnection();
		}
		return instance;
	}
}