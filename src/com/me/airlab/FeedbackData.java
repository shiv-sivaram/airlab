package com.me.airlab;

import java.io.Serializable;

public class FeedbackData implements Serializable {

	/**
	 * @author Sivanand
	 */
	
	public FeedbackData(long sampleSize, long upVotes, long downVotes) {
		
		this.sampleSize = sampleSize;
		this.upVotes = upVotes;
		this.downVotes = downVotes;
	}
	
	public long getSampleSize() {
		return sampleSize;
	}
	public void setSampleSize(long sampleSize) {
		this.sampleSize = sampleSize;
	}
	public long getUpVotes() {
		return upVotes;
	}
	public void setUpVotes(long upVotes) {
		this.upVotes = upVotes;
	}
	public long getDownVotes() {
		return downVotes;
	}
	public void setDownVotes(long downVotes) {
		this.downVotes = downVotes;
	}
	private long sampleSize;
	private long upVotes;
	private long downVotes;
	private static final long serialVersionUID = 8523752930616915589L;
}
