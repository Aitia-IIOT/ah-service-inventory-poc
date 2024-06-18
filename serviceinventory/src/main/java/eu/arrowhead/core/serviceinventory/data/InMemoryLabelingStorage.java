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

package eu.arrowhead.core.serviceinventory.data;

import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.arrowhead.common.dto.shared.LabelingJobStatus;

public class InMemoryLabelingStorage implements ILabelingStorage {

	//=================================================================================================
	// members
	
	private final Logger logger = LogManager.getLogger(InMemoryLabelingStorage.class);
	
	private final Map<UUID,LabelingJob> storage = new ConcurrentHashMap<>();
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	@Override
	public void addJob(final UUID uuid, final LabelingContentType contentType, final String rawContent) throws LabelingStorageException {
		logger.debug("addJob started...");
		
		storage.put(uuid, new LabelingJob(contentType, rawContent));
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public LabelingJob getJob(final UUID uuid) throws LabelingStorageException {
		logger.debug("getJob started...");
		
		return storage.get(uuid);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void updateStatus(final UUID uuid, final LabelingJobStatus status) throws LabelingStorageException {
		logger.debug("updateStatus started...");
		
		if (!storage.containsKey(uuid)) {
			throw new LabelingStorageException("Invalid id");
		}
		
		storage.get(uuid).setStatus(status);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void addError(final UUID uuid, final String error) throws LabelingStorageException {
		logger.debug("addError started...");
		
		if (!storage.containsKey(uuid)) {
			throw new LabelingStorageException("Invalid id");
		}
		
		storage.get(uuid).addErrorMessage(error);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void addResult(final UUID uuid, final Map<String,String> result) throws LabelingStorageException {
		logger.debug("addResult started...");
		
		if (!storage.containsKey(uuid)) {
			throw new LabelingStorageException("Invalid id");
		}
		
		storage.get(uuid).mergeResult(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void setDeleteble(final UUID uuid, final boolean deletable) throws LabelingStorageException {
		logger.debug("setDeleteble started...");
		
		if (storage.containsKey(uuid)) {
			storage.get(uuid).setDeletable(deletable);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public int removeObsoleteJobs() throws LabelingStorageException {
		int count = 0;
		for (final Entry<UUID,LabelingJob> entry : storage.entrySet()) {
			if (entry.getValue().getDeletable()) {
				storage.remove(entry.getKey());
				count++;
			}
		}
		
		return count;
	}
}