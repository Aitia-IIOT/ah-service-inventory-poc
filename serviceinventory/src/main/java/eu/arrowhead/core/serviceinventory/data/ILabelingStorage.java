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
import java.util.UUID;

import eu.arrowhead.common.dto.shared.LabelingJobStatus;

public interface ILabelingStorage {
	
	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void addJob(final UUID uuid, final LabelingContentType contentType, final String rawContent) throws LabelingStorageException;
	public LabelingJob getJob(final UUID uuid) throws LabelingStorageException;
	public void updateStatus(final UUID uuid, final LabelingJobStatus status) throws LabelingStorageException;
	public void addError(final UUID uuid, final String error) throws LabelingStorageException;
	public void addResult(final UUID uuid, final Map<String,String> result) throws LabelingStorageException;
	public void setDeleteble(final UUID uuid, final boolean deletable) throws LabelingStorageException;
	public int removeObsoleteJobs() throws LabelingStorageException;
}