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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.CoreCommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.dto.shared.LabelingJobStatus;
import eu.arrowhead.core.serviceinventory.data.ILabelingStorage;
import eu.arrowhead.core.serviceinventory.data.IScriptConfiguration;
import eu.arrowhead.core.serviceinventory.data.LabelingContentType;
import eu.arrowhead.core.serviceinventory.data.LabelingJob;
import eu.arrowhead.core.serviceinventory.data.LabelingStorageException;

public class LabelingWorker implements Runnable {
	
	//=================================================================================================
	// members
	
	private static final String INPUT_SUFFIX = ".input";
	private static final String OUTPUT_SUFFIX = ".output";
	private static final String ADMINISTRATIVE_KEY_PREFIX = "$";
	private static final String KEY_RESULT = ADMINISTRATIVE_KEY_PREFIX + "Result";
	private static final String SUCCESS_RESULT = "Success";
	private static final String ERROR_RESULT = "Error";
	private static final String KEY_CAUSE = ADMINISTRATIVE_KEY_PREFIX + "Cause";
	private static final String PYTHON = "python";
	
	private final Logger logger = LogManager.getLogger(LabelingWorker.class);
	private final TypeReference<Map<String,Object>> outputTypeRef = new TypeReference<>() {};
	
	private final UUID jobId;
	private String rawContent;
	private LabelingContentType type;
	
	
	@Autowired
	private ILabelingStorage storage;
	
	@Autowired
	private IScriptConfiguration scriptConfig;
	
	@Autowired
	private ObjectMapper mapper;
	
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
			rawContent = job.getRawContent();
			type = job.getType();
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
			if (Thread.currentThread().isInterrupted()) {
				return;
			}
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
			handleError("Can't prepare workspace.", true);
			return null;
		}
		
		final Path inputFile = Path.of(workspace.toString(), jobId.toString() + INPUT_SUFFIX);
		try {
			Files.writeString(inputFile, rawContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			rawContent = null; 
		} catch (final IOException ex) {
			handleError("Can't prepare input file.", true);
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
		command.add(PYTHON);
		command.add(script.getName());
		command.add(inputFilePath.toString());
		command.add(jobId.toString());
		final List<String> additionalParams = scriptConfig.getConfigForScript(script.getName());
		if (!Utilities.isEmpty(additionalParams)) {
			command.addAll(additionalParams);
		}
		
		final File currentDir = script.getParentFile();
		final ProcessBuilder builder = new ProcessBuilder(command);
		builder.directory(currentDir);
		builder.redirectErrorStream(true);
		
		try {
			final Process process = builder.start();
			final String stdOutAndErr = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8); 
			final int exitCode = process.waitFor();
			
			if (exitCode != 0) {
				handleError(script.getName() + ": " + stdOutAndErr, false);
				return 1;
			}
			
			return processResult(script.getName(), inputFilePath.getParent());
		} catch (final IOException | InterruptedException ex) {
			handleError(script.getName() + ": " + ex.getMessage(), false);
			return 1;
		}
	}

	//-------------------------------------------------------------------------------------------------
	private int processResult(final String scriptName, final Path workspace) throws LabelingStorageException {
		logger.debug("LabelingWorker.processResult started...");
		
		final Path outputFile = Path.of(workspace.toString(), jobId.toString() + OUTPUT_SUFFIX);
		if (Files.exists(outputFile)) {
			try {
				final String output = Files.readString(outputFile);
				final Map<String,Object> jsonContent = mapper.readValue(output, outputTypeRef);
				
				if (jsonContent.containsKey(KEY_RESULT) || !(jsonContent.get(KEY_RESULT) instanceof String)) {
					final String resultType = jsonContent.get(KEY_RESULT).toString().trim();
					switch (resultType) {
					case SUCCESS_RESULT:
						handleNormalOutput(scriptName, jsonContent);
						return 0;
					case ERROR_RESULT:
						handleErrorOutput(scriptName, jsonContent);
						return 1;
					default:
						// intentionally do nothing here 
					}
				} 
				
				// missing or invalid result type
				handleError("Invalid output format for script " + scriptName, false);
				return 1;
			} catch (final IOException ex) {
				handleError("Error while parsing output file of script " + scriptName + ": " + ex.getMessage(), false);
				return 1;
			} finally {
				outputFile.toFile().delete();
			}
		}
		
		// file not exits
		handleError( "No output from " + scriptName, false);
		return 1;
	}

	//-------------------------------------------------------------------------------------------------
	private void handleNormalOutput(final String scriptName, final Map<String,Object> jsonContent) throws LabelingStorageException {
		logger.debug("LabelingWorker.handleNormalOutput started...");
		
		final String prefix = type.getPrefix();
		final Map<String,String> output = new HashMap<>();
		for (final Entry<String,Object> entry : jsonContent.entrySet()) {
			if (entry.getKey().startsWith(ADMINISTRATIVE_KEY_PREFIX)) {
				continue;
			}
			
			final String infix = entry.getKey();
			final Object valueObj = entry.getValue();
			
			if (valueObj instanceof List) { // labels in a list
				final List<?> list = (List<?>) valueObj;
				list.stream().forEach((final Object val) -> {
					output.put(prefix + infix + "_" + val.toString(), String.valueOf(true));
				});
			} else if (valueObj instanceof Number) { // for ids and such
				output.put(prefix + infix, valueObj.toString());
			} else { // for individual labels
				output.put(prefix + infix + "_" + valueObj.toString(), String.valueOf(true));
			}
		}

		storage.addResult(jobId, output);
	}

	//-------------------------------------------------------------------------------------------------
	private void handleErrorOutput(final String scriptName, final Map<String,Object> jsonContent) throws LabelingStorageException {
		logger.debug("LabelingWorker.handleErrorOutput started...");
		
		String errorMessage = "Unknown error while running " + scriptName;
		if (jsonContent.containsKey(KEY_CAUSE)) {
			errorMessage = scriptName + ": " + jsonContent.get(KEY_CAUSE).toString();
		}
		
		handleError(errorMessage, false);
	}

	//-------------------------------------------------------------------------------------------------
	private File[] getScriptFiles() throws LabelingStorageException {
		logger.debug("LabelingWorker.getScriptFiles started...");
		
		final File folder = Path.of(scriptFolderPath).toFile();
		if (!folder.exists()) {
			// no scripts
			handleError("No scripts found", true);
			return null;
		}
			
		return folder.listFiles((file) -> !file.isDirectory() && file.getName().endsWith(".py"));
	}
	
	//-------------------------------------------------------------------------------------------------
	private void handleError(final String errorMessage, final boolean statusUpdate) throws LabelingStorageException {
		logger.debug("LabelingWorker.handleError started...");
		
		logger.debug("Error occured during labeling process: {}", errorMessage);
		storage.addError(jobId, errorMessage);
		if (statusUpdate) {
			storage.updateStatus(jobId, LabelingJobStatus.ERROR);
		}
	}
}