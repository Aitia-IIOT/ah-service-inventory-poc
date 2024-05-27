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

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import eu.arrowhead.common.CoreCommonConstants;
import eu.arrowhead.common.dto.shared.LabelingJobStatus;
import eu.arrowhead.core.serviceinventory.data.ILabelingStorage;
import eu.arrowhead.core.serviceinventory.data.LabelingJob;
import eu.arrowhead.core.serviceinventory.data.LabelingStorageException;

public class LabelingWorker implements Runnable {
	
	//=================================================================================================
	// members
	
	private final Logger logger = LogManager.getLogger(LabelingWorker.class);
	
	private final UUID jobId;
	
	@Autowired
	private ILabelingStorage storage;
	
	@Value(CoreCommonConstants.$SERVICE_INVENTORY_WORKING_FOLDER_WD)
	private String workingFolderPath;
	
	@Value(CoreCommonConstants.$SERVICE_INVENTORY_SCRIPT_FOLDER_WD)
	private String scriptFolderPath;
	
	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public LabelingWorker(final UUID jobId) {
		this.jobId = jobId;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void run() {
		logger.trace("LabelingWorker.run started...");
		
		try {
			final LabelingJob job = storage.getJob(jobId);
			if (job == null) {
				// nothing to do
				return;
			}
			
			handleJob();
		} catch (final LabelingStorageException ex) {
			// storage is unavailable
			logger.error("Error while processing labeling job: {}", ex.getMessage());
			logger.debug("Exception: ", ex);
		}
	}
	
	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void handleJob() throws LabelingStorageException {
		logger.trace("LabelingWorker.handleJob started...");
		
		storage.updateStatus(jobId, LabelingJobStatus.IN_PROGRESS);
		
		int errorCounter = 0;
		final File[] scriptFiles = getScriptFiles();
		for (final File script : scriptFiles) {
			errorCounter += runScript(script);
		}
		
		final boolean allErrors = errorCounter == scriptFiles.length; // every script returns with error
		storage.updateStatus(jobId, allErrors ? LabelingJobStatus.ERROR : LabelingJobStatus.FINISHED);
	}
	
	//-------------------------------------------------------------------------------------------------
	private int runScript(final File script) {
		// TODO Auto-generated method stub
		// TODO: continue
		return 0;
	}

	//-------------------------------------------------------------------------------------------------
	private File[] getScriptFiles() throws LabelingStorageException {
		final File folder = Path.of(scriptFolderPath).toFile();
		if (!folder.exists()) {
			// no scripts
			storage.addError(jobId, "No scripts found");
			storage.updateStatus(jobId, LabelingJobStatus.ERROR);
		}
			
		return folder.listFiles((file) -> !file.isDirectory());
	}
}