package mim.renewal.db;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mim.renewal.model.RenewalEntry;
import mim.renewal.model.Subscriber;

import org.apache.log4j.Logger;

public class ChargingHistoryDAO {
	private Logger log = Logger.getLogger(getClass().getName());
	
	protected DatabaseConnection db;
	
	public ChargingHistoryDAO() {
		db = HistoryDatabaseConnection.getInstance();
	}
	
	public void resetEligibleRenewals(String month) {
		CallableStatement stmt = null;
		
		try {
			stmt = this.db.getConnection().prepareCall("{ call resetEligibleRenewals(?, ?) }");
			stmt.setString("month", month);
			
			stmt.executeUpdate();
						
		} catch (SQLException e) {
			log.error("resetEligibleRenewals failed: " + e.getMessage(), e);
		} finally {
			try {
				if (stmt != null) stmt.close();
			} catch (SQLException ex) {
				log.error("failed to close db resources: " + ex.getMessage(), ex);
			}
		}
	}
	
	public void updateBatch(List<RenewalEntry> entries, String month) {

		StringBuilder sb = new StringBuilder();
		
		//String strSqlUpdateSuccess = "UPDATE charge_process SET stat = 2 WHERE a_party IN(" + sb.substring(0, sb.length() - 1) + ")";
		//String strSqlInsertSuccess = "INSERT IGNORE INTO charge_" + month + "(a_party, amount, stat, charge_type, " +
		//		"created, updated, retry_count, sub_type, ref_id, error_code) SELECT a_party, amount, 2, charge_type, created, NOW(), retry_count, " +
		//		"sub_type, ref_id, error_code FROM charge_process WHERE a_party IN(" + sb.substring(0, sb.length() - 1) + ")";
		
		//
		//String strSqlInsert = "INSERT INTO charge_" + month + "(a_party, amount, stat, charge_type, " +
		//		"created, updated, retry_count, sub_type, ref_id, error_code) VALUES('%s', %f, %d, %d, NOW(), NOW(), %d, %d, '%s', '%s');";
		
		
		String strSqlUpdate = "UPDATE charge_process SET retry_count = %d, ref_id = '%s', updated = NOW(), error_code = '%s', stat = %d WHERE id = %d";
		Statement stmt = null;
		int[] rowUpdates = null;
		
		boolean autoCommit = true;
		
		try {
			autoCommit = this.db.getConnection().getAutoCommit();
			this.db.getConnection().setAutoCommit(false);
			stmt = this.db.getConnection().createStatement();
			
			
			long startTime = System.currentTimeMillis();				
			
			for(RenewalEntry entry : entries) {
				
				if(entry.getStat() == 2) {
					sb.append("'");
					sb.append(entry.getaParty());
					sb.append("',");
				}
				
				strSqlUpdate = "UPDATE charge_process SET retry_count = " + entry.getRetryCount() + 
						", ref_id = '" + entry.getReferenceId() + "', updated = NOW(), error_code = '" + entry.getErrorCode() + "', " +
								"stat = " + entry.getStat() + " WHERE id = " + entry.getId();
				
				if(log.isDebugEnabled())
					log.debug("Adding query to batch: " + strSqlUpdate);
				
				stmt.addBatch(strSqlUpdate);
			}
			
			rowUpdates = stmt.executeBatch();
			
			
			updateSuccessRenewals(month, sb.substring(0, sb.length() - 1));
			
			this.db.getConnection().commit();
			
			this.db.getConnection().setAutoCommit(autoCommit);
			
			long stopTime = System.currentTimeMillis();
			long elapsedTime = stopTime - startTime;
			
			if(log.isDebugEnabled())
				log.debug("Batch sql execution completed in (msec)" + elapsedTime);
			
		} catch (SQLException e) {
			log.error("getEligibleRenewal failed: " + e.getMessage(), e);
		} finally {
			try {
				if (stmt != null) stmt.close();
			} catch (SQLException ex) {
				log.error("failed to close db resources: " + ex.getMessage(), ex);
			}
			
			if(rowUpdates != null && rowUpdates.length > 0)
				log.info("Total records executed: " + rowUpdates[0]);
		}
	}
	
	public void updateSuccessRenewals(String month, String aPartyNumbers) {
		CallableStatement stmt = null;
		
		try {
			stmt = this.db.getConnection().prepareCall("{ call updateSuccessRenewals(?, ?) }");
			stmt.setString("month", month);
			stmt.setString("a_partyNumbers", aPartyNumbers);
			
			int rowsAffected = stmt.executeUpdate();
			
			log.info("updateSuccessRenewals() -> Affected rows: " + rowsAffected);
			
		} catch (SQLException e) {
			log.error("updateSuccessRenewals failed: " + e.getMessage(), e);
		} finally {
			try {
				if (stmt != null) stmt.close();
			} catch (SQLException ex) {
				log.error("failed to close db resources: " + ex.getMessage(), ex);
			}			
		}
	}
	
	public List<RenewalEntry> getEligibleRenewal(double chargeAmount, int chargeType) {
		List<RenewalEntry> listRenewals = new ArrayList<RenewalEntry>();
		CallableStatement stmt = null;
		ResultSet rs = null;
		RenewalEntry entry = null;
		
		try {
			stmt = this.db.getConnection().prepareCall("{ call populateAndFetchEligibleRenewals(?, ?) }");
			stmt.setDouble("chargingAmount", chargeAmount);
			stmt.setInt("chargeType", chargeType);
			
			rs = stmt.executeQuery();
			
			while(rs.next()) {
				
				entry = new RenewalEntry();
				entry.setId(rs.getInt("id"));
				entry.setaParty(rs.getString("a_party"));
				entry.setAmount(rs.getDouble("amount"));
				entry.setStat(rs.getInt("stat"));
				entry.setChargeType(rs.getInt("charge_type"));
				entry.setCreated(rs.getDate("created"));
				entry.setUpdated(rs.getDate("updated"));
				entry.setRetryCount(rs.getInt("retry_count"));
				entry.setSubscriberType(rs.getInt("sub_type"));
				entry.setReferenceId(rs.getString("ref_id"));
				entry.setErrorCode(rs.getString("error_code"));
				
				listRenewals.add(entry);
			}
			
			
		} catch (SQLException e) {
			log.error("getEligibleRenewal failed: " + e.getMessage(), e);
		} finally {
			try {
				if(rs != null) rs.close();
				if (stmt != null) stmt.close();
			} catch (SQLException ex) {
				log.error("failed to close db resources: " + ex.getMessage(), ex);
			}
			
			log.info("Total renewl records fetch for processing: " + listRenewals.size());
			
		}
		
		return listRenewals;
	}
	
	public List<Subscriber> getInActiveSubscribers() {
		List<Subscriber> listSubscriber = new ArrayList<Subscriber>();
		CallableStatement stmt = null;
		ResultSet rs = null;
		Subscriber subscriber = null;
		
		try {
			stmt = this.db.getConnection().prepareCall("{ call fetchInActiveSubscribers() }");
			
			rs = stmt.executeQuery();
			
			while(rs.next()) {
				
				subscriber = new Subscriber();
				
				subscriber.setaParty(rs.getString("a_party"));
				subscriber.setInActiveDays(rs.getInt("in_active_days"));
				subscriber.setExpiry(rs.getString("expiry"));
				
				listSubscriber.add(subscriber);
			}
		} catch (SQLException e) {
			log.error("getInActiveSubscribers failed: " + e.getMessage(), e);
		} finally {
			try {
				if(rs != null) rs.close();
				if (stmt != null) stmt.close();
			} catch (SQLException ex) {
				log.error("failed to close db resources: " + ex.getMessage(), ex);
			}			
		}
		
		return listSubscriber;
	}
	
	public Map<String, String> getNotifyCommands() {
		Map<String, String> commandsMapping = new HashMap<String, String>();
		CallableStatement stmt = null;
		ResultSet rs = null;
		
		try {
			stmt = this.db.getConnection().prepareCall("{ call getNotifyCommands() }");
			
			rs = stmt.executeQuery();
			
			while(rs.next()) {
				
				commandsMapping.put(rs.getString("Command"), rs.getString("Xml"));
				
			}
		} catch (SQLException e) {
			log.error("getNotifyCommands failed: " + e.getMessage(), e);
		} finally {
			try {
				if(rs != null) rs.close();
				if (stmt != null) stmt.close();
			} catch (SQLException ex) {
				log.error("failed to close db resources: " + ex.getMessage(), ex);
			}			
		}
		
		return commandsMapping;
	}
}