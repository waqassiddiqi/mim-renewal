package mim.renewal.db;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
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
			stmt = this.db.getConnection().prepareCall("{ call resetEligibleRenewals(?) }");
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

		String strSqlUpdate = "";
		String strSqlInsertSuccess = "";
		String strSqlSubscriberUpdate = "";
		
		Statement stmt = null;
		
		int monthRecords = 0;
		int subscriberRecords = 0;
		int chargeProcessRecords = 0;
		
		try {
			
			stmt = this.db.getConnection().createStatement();
			
			
			long startTime = System.currentTimeMillis();				
			
			for(RenewalEntry entry : entries) {
				
				log.info("Inserting record into mim_history.charge_" + month + ": " + entry.getaParty());
				
				strSqlInsertSuccess = "INSERT IGNORE INTO mim_history.charge_01(a_party, amount, stat, charge_type, created, updated, retry_count, sub_type, ref_id, error_code) SELECT a_party, amount, 2, charge_type, created, NOW(), retry_count, sub_type, ref_id, error_code FROM mim_history.charge_process WHERE a_party = "
						+ "'" + entry.getaParty() + "'";
				
				monthRecords += stmt.executeUpdate(strSqlInsertSuccess);
				
				if(entry.getCos() == 2) {
					
					log.info("Updating subscriber table for Champion user: " + entry.getaParty());
					strSqlSubscriberUpdate = "UPDATE mim.subscriber SET next_renewal_date = DATE_ADD(CURDATE(), INTERVAL 15 DAY), last_success_charge = CURDATE(), updated = NOW(), last_activity = NOW() "
							+ "WHERE a_party = '" + entry.getaParty() + "'";
					
					try {
						subscriberRecords += stmt.executeUpdate(strSqlSubscriberUpdate); 
					} catch(Exception e) {
						log.error(e);
					}
					
				} else {
					
					log.info("Updating subscriber table for standard user: " + entry.getaParty());
					strSqlSubscriberUpdate = "UPDATE mim.subscriber SET next_renewal_date = DATE_ADD(CURDATE(), INTERVAL 7 DAY), last_success_charge = CURDATE(), updated = NOW(), last_activity = NOW() "
							+ "WHERE a_party = '" + entry.getaParty() + "'";
					
					try {
						subscriberRecords += stmt.executeUpdate(strSqlSubscriberUpdate);
					} catch(Exception e) {
						log.error(e);
					}
					
				}
				
				log.info("Updating charge_process table: " + entry.getaParty());
				
				strSqlUpdate = "UPDATE charge_process SET retry_count = " + entry.getRetryCount() + 
						", ref_id = '" + entry.getReferenceId() + "', updated = NOW(), error_code = '" + entry.getErrorCode() + "', " +
								"stat = " + entry.getStat() + " WHERE id = " + entry.getId();
				
				try {
					chargeProcessRecords += stmt.executeUpdate(strSqlUpdate);
				} catch(Exception e) {
					log.error(e);
				}
				
			}			
			
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
			
			log.info("----------------------------------- Summary - " + new Date().toString() + " ---------------------------------");
			log.info("Month table: " + monthRecords + ", Subscriber table: " + subscriberRecords + ", Charge Process table: " + chargeProcessRecords);
			log.info("-------------------------------------------------------------------------------------------------------------");
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
				entry.setCos(rs.getInt("cos"));
				
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