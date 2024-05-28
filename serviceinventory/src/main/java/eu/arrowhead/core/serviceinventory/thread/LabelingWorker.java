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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import eu.arrowhead.common.CoreCommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.dto.shared.LabelingJobStatus;
import eu.arrowhead.core.serviceinventory.data.ILabelingStorage;
import eu.arrowhead.core.serviceinventory.data.IScriptConfiguration;
import eu.arrowhead.core.serviceinventory.data.LabelingJob;
import eu.arrowhead.core.serviceinventory.data.LabelingStorageException;

public class LabelingWorker implements Runnable {
	
	//=================================================================================================
	// members
	
	private static final String INPUT_SUFFIX = ".input";
	private static final String OUTPUT_SUFFIX = ".output";
	
	private final Logger logger = LogManager.getLogger(LabelingWorker.class);
	
	private final UUID jobId;
	
	@Autowired
	private ILabelingStorage storage;
	
	@Autowired
	private IScriptConfiguration scriptConfig;
	
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
		logger.debug("LabelingWorker.run started...");
		
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
		logger.debug("LabelingWorker.handleJob started...");
		
		storage.updateStatus(jobId, LabelingJobStatus.IN_PROGRESS);
		
		int errorCounter = 0;
		final File[] scriptFiles = getScriptFiles();
		if (scriptFiles == null) {
			// no scripts
			return; 
		}
		
		final Path inputFilePath = initializeWorkspace();
		if (inputFilePath == null) {
			// problem with preparing
			return;
		}
		
		for (final File script : scriptFiles) {
			errorCounter += runScript(script, inputFilePath);
		}
		
		cleanWorkspace(inputFilePath.getParent());
		final boolean allErrors = errorCounter == scriptFiles.length; // every script returns with error
		storage.updateStatus(jobId, allErrors ? LabelingJobStatus.ERROR : LabelingJobStatus.FINISHED);
	}
	
	//-------------------------------------------------------------------------------------------------
	private Path initializeWorkspace() throws LabelingStorageException {
		logger.debug("LabelingWorker.initializeWorkspace started...");
		
		final Path workspace = Path.of(workingFolderPath, jobId.toString());
		final boolean success = Utilities.prepareFolder(workspace.toString());
		if (!success) {
			handleError("Can't prepare workspace.");
			return null;
		}
		
		final Path inputFile = Path.of(workspace.toString(), jobId.toString() + INPUT_SUFFIX);
		try {
			Files.writeString(inputFile, storage.getJob(jobId).getRawContent(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (final IOException ex) {
			handleError("Can't prepare input file.");
			return null;
		}
		
		return inputFile;
	}
	
	//-------------------------------------------------------------------------------------------------
	private void cleanWorkspace(final Path workspace) {
		logger.debug("LabelingWorker.cleanWorkspace started...");

		Utilities.clearFolder(workspace.toString());
		workspace.toFile().delete();
	}

	//-------------------------------------------------------------------------------------------------
	private int runScript(final File script, final Path inputFilePath) throws LabelingStorageException {
		logger.debug("LabelingWorker.runScript started...");
		
		final List<String> command = new ArrayList<>();
		command.add(script.getName());
		command.add(inputFilePath.toString());
		command.add(jobId.toString());
		final String additionalParams = scriptConfig.getConfigForScript(script.getName());
		if (!Utilities.isEmpty(additionalParams)) {
			command.add(additionalParams);
		}
		
		final File currentDir = script.getParentFile();
		final ProcessBuilder builder = new ProcessBuilder(command);
		builder.directory(currentDir);
		try {
			final Process process = builder.start();
			process.waitFor();
			
			// TODO: process and delete result
			return 0; // TODO: return value is based on result
		} catch (final IOException | InterruptedException ex) {
			storage.addError(jobId, ex.getMessage());
			return 1;
		}
	}

	//-------------------------------------------------------------------------------------------------
	private File[] getScriptFiles() throws LabelingStorageException {
		logger.debug("LabelingWorker.getScriptFiles started...");
		
		final File folder = Path.of(scriptFolderPath).toFile();
		if (!folder.exists()) {
			// no scripts
			handleError("No scripts found");
			return null;
		}
			
		return folder.listFiles((file) -> !file.isDirectory());
	}
	
	//-------------------------------------------------------------------------------------------------
	private void handleError(final String errorMessage) throws LabelingStorageException {
		logger.debug("LabelingWorker.handleError started...");
		
		logger.debug("Error occured during labeling process: {}", errorMessage);
		storage.addError(jobId, errorMessage);
		storage.updateStatus(jobId, LabelingJobStatus.ERROR);
	}
}