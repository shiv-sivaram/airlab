package com.me.airlab.classifier;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;

/**
 * @author Sivanand
 *
 */
public class Classifier {
	
	public void train(String inputFile) {
		train(inputFile, inputFile+".airltrained");
	}
	
	public void train(String inputFile, String outputFile) {
		train(inputFile, outputFile, "en");
	}
	
	public void train(String inputFile, String outputFile, String languageCode) {
		train(inputFile, outputFile, languageCode, "UTF-8");
	}
	
	public void train(String inputFile, String outputFile, String languageCode, String encoding) {
		
		DoccatModel model = null;

		System.out.println("IN: "+inputFile);
		System.out.println("Out: "+outputFile);
		InputStream dataIn = null;
		try {
			
		  dataIn = new FileInputStream(inputFile);
		  ObjectStream<String> lineStream =	new PlainTextByLineStream(dataIn, encoding);
		  ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(lineStream);

		  System.out.println("SS: "+sampleStream);
		  model = DocumentCategorizerME.train(languageCode, sampleStream);
		}
		catch (IOException e) {
		  // Failed to read or parse training data, training failed
		  e.printStackTrace();
		}
		finally {
		  if (dataIn != null) {
		    try {
		      dataIn.close();
		    }
		    catch (IOException e) {
		      // Not an issue, training already finished.
		      // The exception should be logged and investigated
		      // if part of a production system.
		      e.printStackTrace();
		    }
		  }
		}
		
		OutputStream modelOut = null;
		try {
			
		  modelOut = new BufferedOutputStream(new FileOutputStream(outputFile));
		  model.serialize(modelOut);
		}
		catch (IOException e) {
		  // Failed to save model
		  e.printStackTrace();
		}
		finally {
		  if (modelOut != null) {
		    try {
		       modelOut.close();
		    }
		    catch (IOException e) {
		      // Failed to correctly save model.
		      // Written model might be invalid.
		      e.printStackTrace();
		    }
		  }
		}
	}
	
	public String predict(String inputText, String trainedBinary, String otherCategory, double accuracy) {
		
		try {
		
			InputStream is = new FileInputStream(trainedBinary);
			DoccatModel m = new DoccatModel(is);
			
			DocumentCategorizerME myCategorizer = new DocumentCategorizerME(m);
			double[] outcomes = myCategorizer.categorize(inputText);
			String bestCategory = myCategorizer.getBestCategory(outcomes);
			
			if(bestCategory.equals(otherCategory)) {
				return otherCategory;
			}
			else {
				
				double predictionAccuracy = 1 - (outcomes[myCategorizer.getIndex(bestCategory)] - outcomes[myCategorizer.getIndex(otherCategory)]);
				if(predictionAccuracy > accuracy) {
					return bestCategory;
				}
				else {
					return otherCategory;
				}
			}
		} catch(IOException io) {
			io.printStackTrace();
		}
		return null;
	}
}
