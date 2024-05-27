package eu.arrowhead.core.serviceinventory.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.arrowhead.common.dto.shared.LabelingJobStatus;

public class LabelingJob {
	
	//=================================================================================================
	// members

	private final LabelingContentType type;
	private final String rawContent;
	
	private LabelingJobStatus status;
	private final List<String> errorMessages = new ArrayList<>();
	private final Map<String,String> result = new HashMap<>();
	private boolean deletable = false;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public LabelingJob(final LabelingContentType type, final String rawContent) {
		this.type = type;
		this.rawContent = rawContent;
		this.status = LabelingJobStatus.PENDING;
	}

	//-------------------------------------------------------------------------------------------------
	public LabelingContentType getType() { return type; }
	public String getRawContent() { return rawContent; }
	public LabelingJobStatus getStatus() { return status; }
	public List<String> getErrorMessages() { return errorMessages; }
	public Map<String,String> getResult() { return result; }
	public boolean getDeletable() { return deletable; }

	//-------------------------------------------------------------------------------------------------
	public void setStatus(final LabelingJobStatus status) { this.status = status; }
	public void setDeletable(final boolean deletable) { this.deletable = deletable; }
	
	//-------------------------------------------------------------------------------------------------
	public void addErrorMessage(final String error) {
		errorMessages.add(error);
	}
	
	//-------------------------------------------------------------------------------------------------
	public void mergeResult(final Map<String,String> map) {
		result.putAll(map);
	}
}