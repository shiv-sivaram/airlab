/**
 * 
 */
package com.manageengine.airlab;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sivanand
 * 
 */
public class FeedbackUtil {

	public static FeedbackUtil getInstance() {
		return feedbackUtil;
	}
	
	private FeedbackUtil() {

		thresholdProps = new Properties();
		confidenceAlgos = new Properties();

		try {
			thresholdProps.load(new FileInputStream(thresholdFileName));
			confidenceAlgos.load(new FileInputStream(confidenceAlgosFileName));
		} catch(FileNotFoundException fnf) {
			logger.log(Level.INFO, "Training for the first time ever");
		} catch(IOException io) {
			io.printStackTrace();
		}
	}

	public StatusData updateTrainer(File payload) {

		BufferedInputStream in = null;
		try {

			in = new BufferedInputStream(new FileInputStream(payload));
			String moduleName = TCPUtil.getStringModuleName(in);
			ObjectInputStream ois = new ObjectInputStream(in);
			FeedbackData data = (FeedbackData)ois.readObject();
			setConfidenceScore(moduleName, computeConfidenceScore(moduleName, data));
			
			return new StatusData(200, "Confidence score updated");
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				in.close();
			} catch(IOException io) {
				io.printStackTrace();
			}
		}
		return new StatusData(500, "Server error");
	}

	private double computeConfidenceScore(String moduleName, FeedbackData data) {

		try {
			
			ConfidenceAlgo algo = null;
			String algoClassName = confidenceAlgos.getProperty(moduleName);
			if(algoClassName == null) {
				algoClassName = TCPUtil.getServerProperty("DefaultConfidenceAlgo");
			}
			Class<?> algoClass = Class.forName(algoClassName);
			algo = (ConfidenceAlgo)algoClass.newInstance();
			return algo.computeConfidence(data);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	private void setConfidenceScore(String moduleName, double confidenceScore) {

		thresholdProps.setProperty(moduleName, String.valueOf(confidenceScore));
		try {
			thresholdProps.store(new FileOutputStream(thresholdFileName), "Confidence threshold");
		} catch(IOException io) {
			io.printStackTrace();
		}
	}

	public double getConfidenceScore(String moduleName) {

		String value = thresholdProps.getProperty(moduleName);
		if(value != null) {
			return new Double(value);
		}
		return 0L;
	}
	
	private static FeedbackUtil feedbackUtil = new FeedbackUtil();

	private final String thresholdFileName = "data" + File.separator + "ConfidenceThreshold.props";
	private final String confidenceAlgosFileName = "conf" + File.separator + "ConfidenceAlgos.props";
	private Properties thresholdProps = null;
	private Properties confidenceAlgos = null;
	private Logger logger = Logger.getLogger(com.manageengine.airlab.FeedbackUtil.class.getName());
}
