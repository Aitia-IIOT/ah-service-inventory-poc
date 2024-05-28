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

package eu.arrowhead.core.serviceinventory.thread;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;

import javax.annotation.Resource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.CoreCommonConstants;

@Component
public class LabelingManager extends Thread {

	//=================================================================================================
	// members
	
	private final Logger logger = LogManager.getLogger(LabelingManager.class);

	@Resource(name = CoreCommonConstants.SERVICE_INVENTORY_LABELING_JOB_QUEUE)
	private BlockingQueue<UUID> queue;
	
	@Autowired
	private Function<UUID,LabelingWorker> workerFactory;
	
	@Value(CoreCommonConstants.$SERVICE_INVENTORY_LABEL_WORKER_NUM_WD)
	private int workerNum;
	
	private ThreadPoolExecutor threadPool;
	
	private boolean doWork = false;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public void run() {
		logger.debug("LabelingManager.run started...");
		
		if (doWork) {
			throw new UnsupportedOperationException("LabelingManager is already started.");
		}
		setName(LabelingManager.class.getSimpleName());
		
		threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(workerNum);
		doWork = true;
		
		while (doWork) {
			try {
				final UUID jobId = queue.take();
				threadPool.execute(workerFactory.apply(jobId));
			} catch (final InterruptedException ex) {
				interrupt();
			}
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	@Override
	public void interrupt() {
		doWork = false;
		if (threadPool != null) {
			threadPool.shutdownNow();
		}
		super.interrupt();
	}
}