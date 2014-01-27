package com.me.airlab.classifier;

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.me.airlab.CSVReader;

import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.util.ObjectStream;

public class CSVSampleStream implements ObjectStream {

	public CSVSampleStream(Reader in, int fieldIndex) {
		
		this.in = in;
		csv = new CSVReader(in);
		this.fieldIndex = fieldIndex;
	}
	
	public DocumentSample read() throws IOException {

		try {
			if(!firstLineRead) {

				csv.getAllFieldsInLine();
				firstLineRead = true;
			}

			Vector<String> fields = csv.getAllFieldsInLine();
			return new DocumentSample(fields.elementAt(fieldIndex), fields.elementAt(fields.size() - 1));
		} catch(EOFException eof) {
			
			logger.log(Level.INFO, "Finished reading CSV file for field {0}", fieldIndex);
			return null;
		}
	}

	@Override
	public void close() throws IOException {
		csv.close();
	}

	@Override
	public void reset() throws IOException, UnsupportedOperationException {
		
		in.reset();
		csv = new CSVReader(in);
	}
	
	private int fieldIndex = -1;
	private boolean firstLineRead = false;
	private CSVReader csv = null;
	private Reader in = null;
	
	private Logger logger = Logger.getLogger(com.me.airlab.classifier.CSVSampleStream.class.getName());
}
