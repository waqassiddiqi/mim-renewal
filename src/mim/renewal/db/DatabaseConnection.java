package mim.renewal.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.log4j.Logger;

public abstract class DatabaseConnection {
	private static Connection connection = null;
	private Logger log;
	protected String dbUsername;
	protected String dbPassword;
	protected String dbUrl;
	protected String dbPort;
	protected String dbName;
	
	public DatabaseConnection() {
		log = Logger.getLogger(getClass().getName());
	}
	
	protected abstract void readConfig();
	
	public Connection getConnection() {
		if(!isConnected()) {
			connect();
		} 
		return connection;
	}
	
	public boolean isConnected() {
		try {
			return (connection != null) && !connection.isClosed();
		} catch (SQLException e) {
			log.warn("DatabaseConnection: Connection check failed: " + e.getMessage(), e);
			return false;
		}
	}
	
	public synchronized boolean connect() {
		if(!isConnected()) {
			try {
				readConfig();
				
				Class.forName("com.mysql.jdbc.Driver");
				
				String url = "jdbc:mysql://" + this.dbUrl + ":" + this.dbPort + "/" + this.dbName + "?useServerPrepStmts=false&rewriteBatchedStatements=true&user=" + this.dbUsername 
						+ "&password=" + this.dbPassword;
				
				log.info("DatabaseConnection: Connecting to " + url);
				
				connection = DriverManager.getConnection(url);
				
			} catch (SQLException e) {
				
				log.error("DatabaseConnection: Connection failed: " + e.getMessage(), e);
				return false;
				
			} catch (ClassNotFoundException e) {
				log.error("DatabaseConnection: Driver nod found: " + e.getMessage(), e);
				return false;
			}
		} else {
			log.info("DatabaseConnection: already connected");
		}
		return true;
	}
	
	public synchronized void close() {
		if(isConnected()) {
			try {
				connection.close();
			} catch (SQLException e) {
				log.error("DatabaseConnection: Connection closure failed: " + e.getMessage(), e);
				e.printStackTrace();
			}
		}
	}
}