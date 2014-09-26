package mim.renewal;

import java.util.ResourceBundle;

import mim.renewal.task.ProcessRenewableSubscriberTask;

import org.apache.log4j.Logger;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

public class RenewalDaemon {

	private Logger log = Logger.getLogger(getClass().getName());
	private int renewalCycles = 3;
	private int cycleStart = 8;
	private int cycleEnd = 23;
	
	protected void readConfig() {
		
		log.info("Initializing RenewalDaemon ...");
		
		ResourceBundle myResources = ResourceBundle.getBundle("renewal");
		
		renewalCycles = Integer.parseInt(myResources.getString("renewal.cycles"));
		cycleStart = Integer.parseInt(myResources.getString("renwal.cycle_start_hour"));
		cycleEnd = Integer.parseInt(myResources.getString("renwal.cycle_end_hour"));
	}
	
	public void startDaemon() throws SchedulerException {
		
		readConfig();
		
		JobDetail job = JobBuilder.newJob(ProcessRenewableSubscriberTask.class)
				.withIdentity("dummyJobName", "group1")
				.build();
		
		int hours = ((cycleEnd - cycleStart) / renewalCycles) + 1;
		
		if(cycleEnd == 24)
			cycleEnd = 0;
		
		String cronSchedule = String.format("0 0 %d-%d/%d * * ?", cycleStart, cycleEnd, hours);
		
		Trigger trigger = TriggerBuilder
				.newTrigger()
				.withIdentity("dummyTriggerName", "group1")
				.withSchedule(CronScheduleBuilder.cronSchedule(cronSchedule))
				.startNow()
				.build();
		
		Scheduler scheduler = new StdSchedulerFactory().getScheduler();
    	scheduler.start();
    	scheduler.scheduleJob(job, trigger);
    	
    	scheduler.triggerJob(job.getKey());
    	
    	log.info("RenewalDaemon has started...");
    	
    	log.info("RenewalDaemon will next execute on: " + trigger.getNextFireTime());
	}
	
	public static void main(String[] args) throws SchedulerException {
		new RenewalDaemon().startDaemon();
	}
}