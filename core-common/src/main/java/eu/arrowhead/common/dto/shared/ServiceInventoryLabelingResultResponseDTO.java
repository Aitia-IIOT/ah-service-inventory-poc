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

package eu.arrowhead.common.dto.shared;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class ServiceInventoryLabelingResultResponseDTO implements Serializable {

	//=================================================================================================
	// members
	
	private static final long serialVersionUID = -6251889217059642114L;
	
	private String jobId;
	private LabelingJobStatus status;
	private List<String> errorMessages;
	private Map<String,String> result;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public ServiceInventoryLabelingResultResponseDTO() {}
	
	//-------------------------------------------------------------------------------------------------
	public ServiceInventoryLabelingResultResponseDTO(final String jobId, final LabelingJobStatus status, final List<String> errorMessages, final Map<String,String> result) {
		this.jobId = jobId;
		this.status = status;
		this.errorMessages = errorMessages;
		this.result = result;
	}

	//-------------------------------------------------------------------------------------------------
	public String getJobId() { return jobId; }
	public LabelingJobStatus getStatus() { return status; }
	public List<String> getErrorMessages() { return errorMessages; }
	public Map<String,String> getResult() { return result; }

	//-------------------------------------------------------------------------------------------------
	public void setJobId(final String jobId) { this.jobId = jobId; }
	public void setStatus(final LabelingJobStatus status) { this.status = status; }
	public void setErrorMessages(final List<String> errorMessages) { this.errorMessages = errorMessages; }
	public void setResult(final Map<String,String> result) { this.result = result; }

	//-------------------------------------------------------------------------------------------------
	@Override
	public int hashCode() {
		return Objects.hash(jobId);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		
		if (obj == null) {
			return false;
		}
		
		if (getClass() != obj.getClass()) {
			return false;
		}
		
		final ServiceInventoryLabelingResultResponseDTO other = (ServiceInventoryLabelingResultResponseDTO) obj;
		return Objects.equals(jobId, other.jobId);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "ServiceInventoryLabelingResultResponseDTO [jobId=" + jobId
				+ ", status=" + status + ", errorMessages=" + errorMessages
				+ ", result=" + result + "]";
	}
}