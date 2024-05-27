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

import java.security.InvalidParameterException;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import javax.annotation.Resource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.CoreCommonConstants;
import eu.arrowhead.common.dto.shared.ServiceInventoryLabelingResultResponseDTO;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.core.serviceinventory.data.ILabelingStorage;
import eu.arrowhead.core.serviceinventory.data.LabelingContentType;
import eu.arrowhead.core.serviceinventory.data.LabelingJob;
import eu.arrowhead.core.serviceinventory.data.LabelingStorageException;

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
		
		final UUID uuid = UUID.randomUUID();
		try {
			storage.addJob(uuid, contentType, content);
			labelingJobQueue.add(uuid);
			
			return uuid.toString();
		} catch (final LabelingStorageException ex) {
			throw new ArrowheadException(ex.getMessage(), ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInventoryLabelingResultResponseDTO getLabelingJobResult(final UUID uuid) {
		logger.debug("getLabelingJobResult started..." );
		
		try {
			final LabelingJob job = storage.getJob(uuid);
			if (job == null) { // unknown job id
				throw new InvalidParameterException("Unknown job id: " + uuid);
			}
			
			final ServiceInventoryLabelingResultResponseDTO result = convertJobToResultResponse(uuid, job);
			
			if (result.getStatus().isFinal()) {
				storage.setDeleteble(uuid, true);
			}
			
			return result;
		} catch (final LabelingStorageException ex) {
			throw new ArrowheadException(ex.getMessage(), ex);
		}
	}
	
	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private ServiceInventoryLabelingResultResponseDTO convertJobToResultResponse(final UUID id, final LabelingJob job) {
		logger.debug("convertJobToResultResponse started..." );
		
		final boolean isFinal = job.getStatus().isFinal();
		
		return new ServiceInventoryLabelingResultResponseDTO(id.toString(),
															 job.getStatus(),
															 isFinal ? job.getErrorMessages() : null,
															 isFinal ? job.getResult() : null);
	}
}