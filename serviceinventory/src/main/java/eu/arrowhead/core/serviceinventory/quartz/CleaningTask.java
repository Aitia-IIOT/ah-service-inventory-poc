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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.arrowhead.core.serviceinventory.data.ILabelingStorage;
import eu.arrowhead.core.serviceinventory.data.LabelingStorageException;

@Component
@DisallowConcurrentExecution
public class CleaningTask implements Job {
	
	//=================================================================================================
	// members
	
	protected final Logger logger = LogManager.getLogger(CleaningTask.class);
	
	@Autowired
	private ILabelingStorage storage;
	
	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public void execute(final JobExecutionContext context) throws JobExecutionException {
		logger.debug("STARTED: Cleaning task");
		
		int count;
		try {
			count = storage.removeObsoleteJobs();
			logger.debug("FINISHED: Cleaning task. Number of removed job entry: {}", count);
		} catch (final LabelingStorageException ex) {
			logger.warn("Error occured while removing obsolete job entries: {}", ex.getMessage());
			logger.debug("Exception", ex);
		}
	}
}