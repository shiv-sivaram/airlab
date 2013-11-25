/**
 * 
 */
package com.me.airlab;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

/**
 * @author Sivanand
 *
 */
public final class TCPUtil {
	
	private static Properties serverProps = null;
	private static Properties commands = null;
	private static final String commandsConfFile = "conf" + File.separator + "TCPServerCommands.props";
	private static final String serverConfigFile = "conf" + File.separator + "ServerConfig.props";
	
	private TCPUtil() {
		
	}
	
	static {
		
		serverProps = new Properties();
		commands = new Properties();
		try {
			serverProps.load(new FileInputStream(serverConfigFile));
			commands.load(new FileInputStream(commandsConfFile));
		} catch(IOException io) {
			io.printStackTrace();
		}
	}
	
	public static String getServerProperty(String key) {
		return serverProps.getProperty(key);
	}
	
	public static String getCommand(String key) {
		return commands.getProperty(key);
	}

	public static byte[] getCommandBytes(int command) {
		return intToByte(command, HeaderLengths.COMMAND_LENGTH);
	}
	
	public static byte[] getStatusBytes(int statusCode) {
		return intToByte(statusCode, HeaderLengths.STATUS_CODE_LENGTH);
	}
	
	public static byte[] getSequenceBytes(int sequence) {
		return intToByte(sequence, HeaderLengths.SEQUENCE_LENGTH);
	}
	
	private static byte[] intToByte(int code, int length) {
		return longToByte((long)code, length);
	}
	
	public static int getIntCommand(BufferedInputStream in) throws IOException {
		return (int)byteToLong(readBytes(in, HeaderLengths.COMMAND_LENGTH));
	}
	
	public static int getIntStatus(BufferedInputStream in) throws IOException {
		return (int)byteToLong(readBytes(in, HeaderLengths.STATUS_CODE_LENGTH));
	}
	
	public static int getIntSequence(BufferedInputStream in) throws IOException {
		return (int)byteToLong(readBytes(in, HeaderLengths.SEQUENCE_LENGTH));
	}
	
	public static String getStringFileName(BufferedInputStream in) throws IOException {
		return bytesToString(readBytes(in, HeaderLengths.FILE_NAME_LENGTH));
	}
	
	public static String getStringModuleName(BufferedInputStream in) throws IOException {
		return bytesToString(readBytes(in, HeaderLengths.MODULE_NAME_LENGTH));
	}
	
	public static byte[] getFileNameBytes(String fileName) {
		return stringToBytes(fileName, HeaderLengths.FILE_NAME_LENGTH);
	}
	
	public static byte[] getModuleNameBytes(String moduleName) {
		return stringToBytes(moduleName, HeaderLengths.MODULE_NAME_LENGTH);
	}
	
	public static long getLongPayloadLength(BufferedInputStream in) throws IOException {
		return byteToLong(readBytes(in, HeaderLengths.PAYLOAD_LENGTH));
	}
	
	public static byte[] getPayloadLengthBytes(long length) throws Exception {
		return longToByte(length, HeaderLengths.PAYLOAD_LENGTH);
	}
	
	public static String getPayload(BufferedInputStream in, long payloadLength) {
		
		StringBuilder outputString = new StringBuilder();
		try {

			byte[] buffer = new byte[TCPUtil.BUFFER_LENGTH];
			while(payloadLength > 0) {

				int len = in.read(buffer);
				payloadLength -= len;
				outputString.append(bytesToString(buffer));
			}
		} catch(IOException io) {
			io.printStackTrace();
		}
		return outputString.toString();
	}
	
	public static String getPayload(BufferedInputStream in) throws IOException {

		StringBuilder outputString = new StringBuilder();

		byte[] buffer = new byte[BUFFER_LENGTH];
		while(in.read(buffer) > 0) {
			outputString.append(bytesToString(buffer));
		}
		return outputString.toString();
	}

	private static byte[] readBytes(BufferedInputStream in, int length) throws IOException {
		
		byte [] buffer = new byte[length];
		in.read(buffer, 0, length);
		return buffer;
	}
	
	private static byte[] stringToBytes(String data, int length) {

		byte [] fileByte = data.getBytes();
		byte [] returnBytes = new byte[length];
		for(int i=fileByte.length-1;i>=0;i--) {
			returnBytes[i] = fileByte[i];
		}
		return returnBytes;
	}
	
	private static String bytesToString(byte [] data) {
		
		int i=0;
		for(;i<data.length;i++) {
			if(data[i] == 0) {
				break;
			}
		}
		return new String(Arrays.copyOfRange(data, 0, i));
	}
	
	private static long byteToLong(byte[] bytes) {
		
		long returnValue = 0L;
		for(int i=0;i<bytes.length;i++) {
			
			returnValue <<= 7;
			returnValue += bytes[i];
		}
		return returnValue;
	}
	
	private static byte[] longToByte(long data, int length) {
		
		byte[] returnValue = new byte[length];
		long mask = 127;
		for(int i=returnValue.length;i>0;i--) {
			returnValue[i-1] = (byte) (data & mask);
			data >>= 7;
			
		}
		return returnValue;
	}
	
	public static final int BUFFER_LENGTH = 1024;
	public static final long CHUNK_SIZE = 1024 * 1024;
	public static final int RETRY_COUNT = 5;
	
	public class HeaderLengths {
	
		public static final int FILE_NAME_LENGTH = 40;
		public static final int MODULE_NAME_LENGTH = 40;
		public static final int PAYLOAD_LENGTH = 4;
		public static final int SEQUENCE_LENGTH = 2;
		public static final int STATUS_CODE_LENGTH = 2;
		public static final int COMMAND_LENGTH = 2;
	}
	
	public class Command {
		
		public static final int TRANSMIT_FILE = 8;
		public static final int MERGE_FILES = 9;
		public static final int TRAIN_CLASSIFIER = 10;
		public static final int DELTA_TRAIN_CLASSIFIER = 11;
		public static final int GET_LAST_TRAINED = 12;
		public static final int TRANSMIT_FEEDBACK = 30;
		public static final int PREDICT_CATEGORY = 20;
		
	}
	
	public class Status {
		
		public static final int OK = 200;
		public static final int SERVER_ERROR = 500;
		public static final int NOT_TRAINED = 501;
		public static final int NO_FILES = 502;
	}
}
