package mim.renewal.task;

import java.util.List;
import java.util.Map;

import mim.provgw.Client;
import mim.renewal.db.ChargingHistoryDAO;
import mim.renewal.model.Subscriber;
import mim.renewal.util.Util;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

public class NotifyInActiveSubscriberTask implements Job {
	private ChargingHistoryDAO chargingDao = new ChargingHistoryDAO();
	private Logger log = Logger.getLogger(getClass().getName());
	private Client client;
	
	public void execute(JobExecutionContext context) {
		
		log.info("Executing NotifyInActiveSubscriberTask... ");
		
		client = new Client();
		
		Map<String, String> xmlCommands = chargingDao.getNotifyCommands();
		
		List<Subscriber> listSubscribers = chargingDao.getInActiveSubscribers();
		String xmlRequest = "";
		
		for(Subscriber s : listSubscribers) {
			if(s.getInActiveDays() < 7) {
				xmlRequest = xmlCommands.get("NOTIFY IN ACTIVE SUBSCRIBER");
				
				if(xmlRequest != null) {
					xmlRequest = xmlRequest.replace("&1", s.getExpiry());
				}
				
			} else {
				xmlRequest = xmlCommands.get("AUTO UNSUB");
				
				if(xmlRequest != null) {
					xmlRequest = xmlRequest.replace("&1", "7");
				}
			}
			
			if(xmlRequest != null) {
				xmlRequest = xmlRequest.replace("#DestAddr#", "6060");
				xmlRequest = xmlRequest.replace("#apartyMSISDN#", s.getaParty());
				
				String refId = Util.generateReferenceId();
				
				xmlRequest = xmlRequest.replace("#GwMsgId#", refId);
				
				client.sendRequest(xmlRequest, refId);
			}
		}
	}
}