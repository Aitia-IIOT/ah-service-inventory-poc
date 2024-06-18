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

public enum LabelingContentType {
	INPUT, OUTPUT;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public String getPrefix() {
		switch (this) {
		case INPUT: return "I";
		case OUTPUT: return "O";
		default:
			throw new IllegalStateException();
		}
	}
}
