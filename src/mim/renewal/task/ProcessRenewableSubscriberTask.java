package mim.renewal.task;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

import mim.renewal.db.ChargingHistoryDAO;
import mim.renewal.model.RenewalEntry;
import mim.renewal.util.PreferenceUtil;
import mim.ucip.Client;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

public class ProcessRenewableSubscriberTask implements Job {
	
	private JobExecutionContext context;
	private ChargingHistoryDAO chargingDao = new ChargingHistoryDAO();
	final SimpleDateFormat sdf = new SimpleDateFormat("MM");
	private Logger log = Logger.getLogger(getClass().getName());
	private static double chargeAamount = 2.50;
	private static int chargeType = 1;
	
	static {
		ResourceBundle myResources = ResourceBundle.getBundle("renewal");
		
		chargeAamount = Double.parseDouble(myResources.getString("renewal.charge_amount"));
		chargeType = Integer.parseInt(myResources.getString("renewal.charge_type"));
	}
	
	public void execute(JobExecutionContext context) {
		
		this.context = context;
		
		List<RenewalEntry> listRenewalEntries = null;
		
		if(PreferenceUtil.getInt(PreferenceUtil.PREF_APP_ITERATION, 1) == 1) {
			listRenewalEntries = chargingDao.getEligibleRenewal(chargeAamount, chargeType);
		}
		
		if(listRenewalEntries != null) {
			new Client().sendRequest(listRenewalEntries);
			
			chargingDao.updateBatch(listRenewalEntries, sdf.format(new Date()));
		}
		
		cleanUp();
		
		if(context.getNextFireTime() != null)
			log.info("Renewal deamon will next fire at: " + context.getNextFireTime());
	}
	
	private void cleanUp() {
		if(context.getNextFireTime() != null) {
			Calendar calNextFire = Calendar.getInstance();
	    	calNextFire.setTime(context.getNextFireTime());
	
	
	    	Calendar calToday = Calendar.getInstance();
	    	
	    	if(calNextFire.get(Calendar.DAY_OF_YEAR) != calToday.get(Calendar.DAY_OF_YEAR)) {
	    		log.info("Clearing todays renwal entries..");
	    		
	    		chargingDao.resetEligibleRenewals(sdf.format(new Date()));
	    	}
		}
	}
}