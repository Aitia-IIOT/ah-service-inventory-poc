/********************************************************************************
 * Copyright (c) 2024 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   AITIA - implementation
 *   Arrowhead Consortia - conceptualization
 ********************************************************************************/

package eu.arrowhead.core.serviceinventory.quartz;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.SimpleTrigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

import eu.arrowhead.common.CoreCommonConstants;
import eu.arrowhead.common.quartz.AutoWiringSpringBeanQuartzTaskFactory;

@Configuration
public class CleaningTaskConfig {
	
	//=================================================================================================
	// members

	protected final Logger logger = LogManager.getLogger(CleaningTaskConfig.class);
	
	@Autowired
    private ApplicationContext applicationContext; 
	
	@Value(CoreCommonConstants.$SERVICE_INVENTORY_CLEAN_INTERVAL_WD)
	private int interval;
	
	private static final int SCHEDULER_DELAY = 10;
	private static final String NUM_OF_THREADS = "1";
	
	private static final String NAME_OF_TRIGGER = "Clean_Task_Trigger";
	private static final String NAME_OF_TASK = "Clean_Task_Detail";
	
	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Bean
    public SchedulerFactoryBean cleaningTaskScheduler() {
		final AutoWiringSpringBeanQuartzTaskFactory jobFactory = new AutoWiringSpringBeanQuartzTaskFactory();
		jobFactory.setApplicationContext(applicationContext);
		final SchedulerFactoryBean schedulerFactory = new SchedulerFactoryBean();
		final Properties schedulerProperties = new Properties();     
		schedulerProperties.put(CoreCommonConstants.QUARTZ_THREAD_PROPERTY, NUM_OF_THREADS);
	    schedulerFactory.setQuartzProperties(schedulerProperties);
		
        schedulerFactory.setJobFactory(jobFactory);
        schedulerFactory.setJobDetails(cleaningTaskDetail().getObject());
	    schedulerFactory.setTriggers(cleaningTaskTrigger().getObject());
	    schedulerFactory.setStartupDelay(SCHEDULER_DELAY);
	    logger.info("Cleaner task adjusted with interval: {} minutes", interval);
		
		return schedulerFactory;        
    }
	
	//-------------------------------------------------------------------------------------------------
	@Bean
    public SimpleTriggerFactoryBean cleaningTaskTrigger() {
		final SimpleTriggerFactoryBean trigger = new SimpleTriggerFactoryBean();
		trigger.setJobDetail(cleaningTaskDetail().getObject());
        trigger.setRepeatInterval(interval * CoreCommonConstants.CONVERSION_MILLISECOND_TO_MINUTE);
        trigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
        trigger.setName(NAME_OF_TRIGGER);
        
        return trigger;
    }

    //-------------------------------------------------------------------------------------------------
    @Bean
    public JobDetailFactoryBean cleaningTaskDetail() {
        final JobDetailFactoryBean jobDetailFactory = new JobDetailFactoryBean();
        jobDetailFactory.setJobClass(CleaningTask.class);
        jobDetailFactory.setName(NAME_OF_TASK);
        jobDetailFactory.setDurability(true);
        
        return jobDetailFactory;
    }
}