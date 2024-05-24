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

package eu.arrowhead.core.serviceinventory.service;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import javax.annotation.Resource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.CoreCommonConstants;
import eu.arrowhead.core.serviceinventory.data.ILabelingStorage;
import eu.arrowhead.core.serviceinventory.data.LabelingContentType;

@Service
public class ServiceInventoryService {
	
	//=================================================================================================
	// members
	
	private final Logger logger = LogManager.getLogger(ServiceInventoryService.class);
	
	@Autowired
	private ILabelingStorage storage;
	
	@Resource(name = CoreCommonConstants.SERVICE_INVENTORY_LABELING_JOB_QUEUE)
	private BlockingQueue<UUID> labelingJobQueue;
	
	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public String registerLabelingJob(final LabelingContentType contentType, final String content) {
		logger.debug("registerLabelingJob started..." );
		
		// TODO: continue
		
		return null;
	}

}
