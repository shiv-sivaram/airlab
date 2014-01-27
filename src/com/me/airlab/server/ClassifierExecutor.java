package com.me.airlab.server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import com.me.airlab.CSVReader;
import com.me.airlab.FeedbackUtil;
import com.me.airlab.StatusData;
import com.me.airlab.TCPUtil;
import com.me.airlab.classifier.Classifier;

/**
 * @author Sivanand
 * 
 */
public class ClassifierExecutor {
	
	public ClassifierExecutor() {
		
		lastTrained = new Properties();
		
		try {
			lastTrained.load(new FileInputStream(lastTrainedFileName));
		} catch(FileNotFoundException fnf) {
			logger.log(Level.INFO, "Training for the first time ever");
		}
		catch(IOException io) {
			io.printStackTrace();
		}
	}

	public StatusData trainClassifier(File payload) {

		String module = createRawFile(payload, false);
		return train(module);
	}

	public StatusData deltaTrainClassifier(File payload) {
		
		String module = createRawFile(payload, true);
		return train(module);
	}
	
	private StatusData train(String module) {

		long threadId = Thread.currentThread().getId();
		logger.log(Level.INFO, "["+threadId+"]Training started");

		long lastTrainedTime = 0L;
		Classifier c = new Classifier();
		c.train("data" + File.separator + "raw" + File.separator + module + ".airlraw", "data" + File.separator + "trained" + File.separator + module + ".airltrained");

		logger.log(Level.INFO, "["+threadId+"]Training done");
		
		lastTrainedTime = System.currentTimeMillis();
		setLastTrained(module, lastTrainedTime);
		
		return new StatusData(TCPUtil.Status.OK, String.valueOf(lastTrainedTime));
	}

	public StatusData trainMultiClassifier(File payload) {

		String module = createRawFile(payload, true);
		return trainMulti(module);
	}

	public StatusData deltaTrainMultiClassifier(File payload) {

		String module = createRawFile(payload, false);
		return trainMulti(module);
	}
	
	private StatusData trainMulti(String module) {

		FileReader fReader = null;
		CSVReader csv = null;

		try {

			fReader = new FileReader("data" + File.separator + "raw" + File.separator + module + ".airlraw");
			csv = new CSVReader(fReader);
			Vector headers = csv.getAllFieldsInLine();

			String[] submodules = new String[headers.size() - 1];
			for(int i = 0;i < headers.size() - 1;i++) {
				submodules[i] = (String) headers.elementAt(i);
			}

			long threadId = Thread.currentThread().getId();
			logger.log(Level.INFO, "[" + threadId + "]Training started");

			long lastTrainedTime = 0L;
			Classifier c = new Classifier();
			c.trainMulti("data" + File.separator + "raw" + File.separator + module + ".airlraw", "data" + File.separator + "trained" + File.separator + module, submodules);

			logger.log(Level.INFO, "[" + threadId + "]Training done");

			lastTrainedTime = System.currentTimeMillis();
			setLastTrained(module, lastTrainedTime);

			return new StatusData(TCPUtil.Status.OK, String.valueOf(lastTrainedTime));
		} catch(Exception e) {
			e.printStackTrace();
		} finally {

			try {
				csv.close();
			} catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		return new StatusData(TCPUtil.Status.SERVER_ERROR, "Error training");
	}

	public StatusData getLastTrained(File payload) {
		
		BufferedInputStream in = null;
		try {
			
			in = new BufferedInputStream(new FileInputStream(payload));
			String moduleName = TCPUtil.getStringModuleName(in);
			return new StatusData(TCPUtil.Status.OK, String.valueOf(getLastTrained(moduleName)));
		} catch(FileNotFoundException fnf) {
			fnf.printStackTrace();
			return new StatusData(TCPUtil.Status.OK, "0");
		} catch(IOException io) {
			io.printStackTrace();
			return new StatusData(TCPUtil.Status.SERVER_ERROR, "Internal server error");
		} finally {
			try {
				in.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	private String createRawFile(File payload, boolean append) {

		File fTmpFile = null;
		FileInputStream tmpFile = null;
		FileOutputStream rawFile = null;
		try {

			BufferedInputStream in = new BufferedInputStream(new FileInputStream(payload));
			String fileName = TCPUtil.getStringFileName(in);
			String moduleName = TCPUtil.getStringModuleName(in);
			
			logger.log(Level.INFO, "File name {0}", fileName);
			logger.log(Level.INFO, "Module name {0}", moduleName);
			
			fTmpFile = new File("tmp" + File.separator + fileName);
			tmpFile = new FileInputStream(fTmpFile);
			rawFile = new FileOutputStream("data" + File.separator + "raw" + File.separator + moduleName + ".airlraw");
			
			byte [] buffer = new byte[TCPUtil.BUFFER_LENGTH];
			int len = 0;
			while((len = tmpFile.read(buffer)) > 0) {
				rawFile.write(buffer, 0, len);
			}
			
			return moduleName;
		} catch(IOException io) {
			io.printStackTrace();
		} finally {
			try {
			tmpFile.close();
			rawFile.close();
			fTmpFile.delete();
			} catch(IOException io) {
				io.printStackTrace();
			}
		}
		return null;
	}

	public StatusData predictCategory(File payload) {

		try {

			BufferedInputStream in = new BufferedInputStream(new FileInputStream(payload));
			String module = TCPUtil.getStringModuleName(in);

			if(getLastTrained(module) == 0L) {
				return new StatusData(TCPUtil.Status.NOT_TRAINED, "Not Trained");
			}

			String payloadData[] = TCPUtil.getPayload(in).split(TCPUtil.FIELD_SEPARATOR);
			Classifier c = new Classifier();
			String prediction = c.predict(payloadData[0], "data" + File.separator + "trained" + File.separator + module + ".airltrained", payloadData[1], FeedbackUtil.getInstance().getConfidenceScore(module));
			return new StatusData(TCPUtil.Status.OK, prediction);
		} catch(FileNotFoundException fnf) {
			fnf.printStackTrace();
			return new StatusData(TCPUtil.Status.SERVER_ERROR, "File not found");
		} catch(IOException io) {
			io.printStackTrace();
			return new StatusData(TCPUtil.Status.SERVER_ERROR, "IO Error");
		}
	}
	
	public StatusData predictMultiCategories(File payload) {

		try {

			BufferedInputStream in = new BufferedInputStream(new FileInputStream(payload));
			String module = TCPUtil.getStringModuleName(in);

			if(getLastTrained(module) == 0L) {
				return new StatusData(TCPUtil.Status.NOT_TRAINED, "Not Trained");
			}

			String payloadData = TCPUtil.getPayload(in);
			int lineIndex = payloadData.indexOf("\r\n");
			String submodulesStr = payloadData.substring(0, lineIndex);
			int otherCategoriesIndex = payloadData.indexOf("\r\n", lineIndex + 1);
			String otherCategoriesStr = payloadData.substring(lineIndex + 1, otherCategoriesIndex);
			String data = payloadData.substring(otherCategoriesIndex + 1, payloadData.length());
			
			Classifier c = new Classifier();
			HashMap<String, String> predictions = c.predictMulti(data, "data" + File.separator + "trained" + File.separator + module, submodulesStr.split(","), otherCategoriesStr.split(","), FeedbackUtil.getInstance().getConfidenceScore(module, submodulesStr.split(",")));

			JSONObject predictionsJSON = new JSONObject();
			for(Entry<String, String> prediction : predictions.entrySet()) {
				
				try {
					predictionsJSON.put(prediction.getKey(), prediction.getValue());
				} catch(JSONException jx) {
					jx.printStackTrace();
				}
			}
			return new StatusData(TCPUtil.Status.OK, predictionsJSON.toString());
		} catch(FileNotFoundException fnf) {
			fnf.printStackTrace();
			return new StatusData(TCPUtil.Status.SERVER_ERROR, "File not found");
		} catch(IOException io) {
			io.printStackTrace();
			return new StatusData(TCPUtil.Status.SERVER_ERROR, "IO Error");
		}
	}
	
	private long getLastTrained(String moduleName) {
		
		String value = lastTrained.getProperty(moduleName);
		if(value != null) {
			return new Long(value);
		}
		return 0L;
	}
	
	private void setLastTrained(String moduleName, long timestamp) {
		
		lastTrained.setProperty(moduleName, String.valueOf(timestamp));
		try {
			lastTrained.store(new FileOutputStream(lastTrainedFileName), "Last Trained Times");
		} catch(IOException io) {
			io.printStackTrace();
		}
	}

	private final String lastTrainedFileName = "data" + File.separator + "LastTrained.props";
	private Properties lastTrained = null;
	
	private Logger logger = Logger.getLogger(com.me.airlab.server.ClassifierExecutor.class.getName());
}
