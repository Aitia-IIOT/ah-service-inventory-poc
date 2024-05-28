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
import java.util.concurrent.ConcurrentHashMap;

public class HardcodedScriptConfiguration implements IScriptConfiguration {
	
	//=================================================================================================
	// members
	
	private final Map<String,String> config = new ConcurrentHashMap<>();
	
	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public HardcodedScriptConfiguration() {
		// TODO: add config strings of different scripts
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String getConfigForScript(final String scriptName) {
		return config.get(scriptName);
	}
}