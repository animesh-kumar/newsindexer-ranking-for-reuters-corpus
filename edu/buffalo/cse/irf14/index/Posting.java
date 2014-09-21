package edu.buffalo.cse.irf14.index;

public class Posting {

	/**
	 * Document Id generated by sequence
	 */
	private Integer docId;
	/**
	 * File Id
	 */
	private String fileId;
	/**
	 * no. of occurences
	 */
	private Integer frequency;

	/**
	 * @return the docId
	 */
	public Integer getDocId() {
		return docId;
	}

	/**
	 * @param docId
	 *            the docId to set
	 */
	public void setDocId(Integer docId) {
		this.docId = docId;
	}

	/**
	 * @return the fileId
	 */
	public String getFileId() {
		return fileId;
	}

	/**
	 * @param fileId
	 *            the fileId to set
	 */
	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

	/**
	 * @return the frequency
	 */
	public Integer getFrequency() {
		return frequency;
	}

	/**
	 * @param frequency
	 *            the frequency to set
	 */
	public void setFrequency(Integer frequency) {
		this.frequency = frequency;
	}
	
	/**
	 * Overriding equals.To check if a postings list contains a posting of a particular
	 * file id
	 */
	@Override
	public boolean equals(Object object){
		if(object instanceof Posting){
			Posting posting = (Posting) object;
			if(object instanceof Posting
					&& this.getDocId()!=null
					&& this.getDocId().equals(posting.getDocId())){
				return true;
			}
			
		}
		return false;
	}

}
