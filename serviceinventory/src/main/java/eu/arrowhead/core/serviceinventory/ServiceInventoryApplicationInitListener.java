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

package eu.arrowhead.core.serviceinventory;

import java.io.File;
import java.nio.file.Path;
import java.util.ServiceConfigurationError;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.ApplicationInitListener;
import eu.arrowhead.common.CoreCommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.core.serviceinventory.thread.LabelingManager;

@Component
public class ServiceInventoryApplicationInitListener extends ApplicationInitListener {

	//=================================================================================================
	// members
	
	@Value(CoreCommonConstants.$SERVICE_INVENTORY_WORKING_FOLDER_WD)
	private String workingFolderPath;
	
	@Value(CoreCommonConstants.$SERVICE_INVENTORY_SCRIPT_FOLDER_WD)
	private String scriptFolderPath;
	
	@Autowired
	private LabelingManager labelingManager;
	
	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customInit(final ContextRefreshedEvent event) {
		logger.debug("customInit started...");
		
		boolean result = Utilities.prepareFolder(workingFolderPath);
		if (!result) {
			throw new ServiceConfigurationError("Working folder creation failed.");
		}
		Utilities.clearFolder(workingFolderPath);
	
		// TODO: uncomment this
//		checkScriptFolder();
		
		labelingManager.start();
	}
	
	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customDestroy() {
		logger.debug("customDestroy started...");

		labelingManager.interrupt();
	}
	
	//-------------------------------------------------------------------------------------------------
  	private void checkScriptFolder() {
  		if (!Utilities.isEmpty(scriptFolderPath)) {
  			final File folder = Path.of(scriptFolderPath).toFile();
  			if (!folder.exists()) {
  				throw new ServiceConfigurationError("Script folder is not exists.");
  			}
  			
  			final File[] files = folder.listFiles((file) -> !file.isDirectory());
  			if (files.length == 0) {
  				throw new ServiceConfigurationError("Script folder does not contain any scripts.");
  			}
  		} else {
  			throw new ServiceConfigurationError("Script folder is not defined.");
  		}
  	}
}
