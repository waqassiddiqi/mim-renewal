package mim.renewal.db;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import mim.renewal.model.RenewalEntry;

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

		Statement stmt = null;
		String strSqlInsert = "INSERT INTO charge_" + month + "(a_party, amount, stat, charge_type, " +
				"created, updated, retry_count, sub_type, ref_id, error_code) VALUES('%s', %f, %d, %d, NOW(), NOW(), %d, %d, '%s', '%s');";
		
		
		String strSqlUpdate = "UPDATE charge_process SET retry_count = %d, ref_id = '%s', updated = NOW(), error_code = '%s', stat = %d WHERE id = %d";
		
		int[] rowUpdates = null;
		
		try {
			
			this.db.getConnection().setAutoCommit(false);
			stmt = this.db.getConnection().createStatement();
			
			
			long startTime = System.currentTimeMillis();
			
			for(RenewalEntry entry : entries) {
				
				if(entry.getStat() == 2) {
					//strSql = String.format(strSqlInsert, entry.getaParty(), entry.getAmount(), entry.getStat(), entry.getChargeType(), 
					//		entry.getRetryCount(), entry.getSubscriberType(), entry.getReferenceId(), entry.getErrorCode());
					
					strSqlInsert = "INSERT INTO charge_" + month + "(a_party, amount, stat, charge_type, " +
							"created, updated, retry_count, sub_type, ref_id, error_code) VALUES('" + entry.getaParty() + 
							"', " + entry.getAmount() + ", " + entry.getStat() + "," + entry.getChargeType() + ", NOW(), NOW(), " +
							entry.getRetryCount() + ", " + entry.getSubscriberType() + ", '" + entry.getReferenceId() + "', '" + entry.getErrorCode() + "');";
					
					
					if(log.isDebugEnabled())
						log.debug("Adding query to batch: " + strSqlInsert);
					
					stmt.addBatch(strSqlInsert);
				}
				
				//strSql = String.format(strSqlUpdate, (entry.getStat() != 2) ? entry.getRetryCount() + 1 : entry.getRetryCount(), 
				//			entry.getReferenceId(), entry.getErrorCode(), entry.getStat(), entry.getId());
				
				strSqlUpdate = "UPDATE charge_process SET retry_count = " + ((entry.getStat() != 2) ? entry.getRetryCount() + 1 : entry.getRetryCount()) + 
						", ref_id = '" + entry.getReferenceId() + "', updated = NOW(), error_code = '" + entry.getErrorCode() + "', " +
								"stat = " + entry.getStat() + " WHERE id = " + entry.getId();
				
				if(log.isDebugEnabled())
					log.debug("Adding query to batch: " + strSqlUpdate);
				
				stmt.addBatch(strSqlUpdate);
				
				rowUpdates = stmt.executeBatch();
				
				this.db.getConnection().commit();
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
			
			if(rowUpdates != null && rowUpdates.length > 0)
				log.info("Total records executed: " + rowUpdates[0]);
		}
	}
	
	public List<RenewalEntry> getEligibleRenewal() {
		List<RenewalEntry> listRenewals = new ArrayList<RenewalEntry>();
		CallableStatement stmt = null;
		ResultSet rs = null;
		RenewalEntry entry = null;
		
		try {
			stmt = this.db.getConnection().prepareCall("{ call populateAndFetchEligibleRenewals(?, ?) }");
			stmt.setDouble("chargingAmount", 2.50);
			stmt.setInt("chargeType", 1);
			
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
}