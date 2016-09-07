package org.jbpm.services.cdi.impl.store;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.AccessTimeout;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Lock;
import javax.ejb.LockType;
//import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;

import org.jbpm.kie.services.impl.store.DeploymentSynchronizer;

@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@Lock(LockType.WRITE)
@TransactionManagement(TransactionManagementType.BEAN)
@AccessTimeout(value=1, unit=TimeUnit.MINUTES)
public class DeploymentSynchronizerCDInvoker {
	
	private Timer timer;
	@Resource
    private TimerService timerService;
	@Inject
	private DeploymentSynchronizer deploymentSynchronizer;
	
	@PostConstruct
	public void configure() {
//		if (DeploymentSynchronizer.DEPLOY_SYNC_ENABLED) {
//			ScheduleExpression schedule = new ScheduleExpression();
//			
//			schedule.hour("*");
//			schedule.minute("*");
//			schedule.second("*/" + DeploymentSynchronizer.DEPLOY_SYNC_INTERVAL);
//			timer = timerService.createCalendarTimer(schedule, new TimerConfig(null, false));
//		}
	    //2016-9-7, PTI does not use the jBPM console UI to create kjar. PTI seldom deploy kjar during bpm running. so it only needs single-action timer
	    timer = timerService.createSingleActionTimer(60000, new TimerConfig(null, false));
	}
	
	@PreDestroy
	public void shutdown() {
		if (timer != null) {
			timer.cancel();
		}
	}

	@Timeout
	public void synchronize() {
		deploymentSynchronizer.synchronize();
	}

	
}
