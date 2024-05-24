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
