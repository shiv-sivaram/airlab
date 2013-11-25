package com.manageengine.airlab;

public class DefaultConfidenceAlgoImpl implements ConfidenceAlgo {
	
	public double computeConfidence(FeedbackData data) {
		return ((double)data.getUpVotes()) / (double)((data.getUpVotes() + data.getDownVotes()));
	}
}
