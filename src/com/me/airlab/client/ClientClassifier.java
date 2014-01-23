/**
 * 
 */
package com.me.airlab.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.me.airlab.FeedbackData;
import com.me.airlab.TCPUtil;
import com.me.airlab.TCPUtil.HeaderLengths;

/**
 * @author Sivanand
 *
 */
public class ClientClassifier {
	
	public ClientClassifier(String host, int port) {
		
		this.host = host;
		this.port = port;
	}
	
	public long trainClassifier(String moduleName, String filePath) throws Exception {
		
		if(sendFile(filePath)) {
			logger.log(Level.INFO, "Files chunked and sent to server");
		}
		if(mergeFiles(filePath)) {
			logger.log(Level.INFO, "Files merged on server");
		}
		return train(moduleName, filePath, TCPUtil.Command.TRAIN_CLASSIFIER);
	}
	
	public long trainMultiClassifier(String moduleName, String filePath) throws Exception {
		
		if(sendFile(filePath)) {
			logger.log(Level.INFO, "Files chunked and sent to server");
		}
		if(mergeFiles(filePath)) {
			logger.log(Level.INFO, "Files merged on server");
		}
		return train(moduleName, filePath, TCPUtil.Command.TRAIN_MULTI_CLASSIFIER);
	}
	
	public String predictCategory(String moduleName, File textFile) throws Exception {
		
		StringBuilder data = new StringBuilder();
		BufferedReader in = new BufferedReader(new FileReader(textFile));
		String line = in.readLine();
		while(line != null) {
			data.append(line);
			line = in.readLine();
		}
		return predictCategory(moduleName, data.toString());
	}
	
	public String predictCategory(String moduleName, String text) throws Exception {
		
		Socket socket = null;
		BufferedInputStream in = null;
		BufferedOutputStream out = null;
		
		try {
			
			socket = new Socket(host, port);
			in = new BufferedInputStream(socket.getInputStream());
			out = new BufferedOutputStream(socket.getOutputStream());
			
			out.write(TCPUtil.getCommandBytes(TCPUtil.Command.PREDICT_CATEGORY));
			byte [] payload = text.getBytes();
			byte [] moduleBytes = TCPUtil.getModuleNameBytes(moduleName);
			out.write(TCPUtil.getPayloadLengthBytes(payload.length + moduleBytes.length));
			out.write(moduleBytes);
			out.write(payload);
			out.flush();
			
			int status = TCPUtil.getIntStatus(in);
			long payloadLength = TCPUtil.getLongPayloadLength(in);
			
			logger.log(Level.INFO, "Status {0}", status);
			logger.log(Level.INFO, "Payload Length {0}", payloadLength);
			
			String category = TCPUtil.getPayload(in, payloadLength);
			return category;
		} catch(IOException io) {
			io.printStackTrace();
		} finally {
			try {
				
				in.close();
				out.close();
				socket.close();
			} catch(IOException io) {
				io.printStackTrace();
			}
		}
		return null;
	}
	
	public long getLastTrained(String moduleName) throws Exception {
		
		Socket socket = null;
		BufferedInputStream in = null;
		BufferedOutputStream out = null;
		
		try {
			
			socket = new Socket(host, port);
			in = new BufferedInputStream(socket.getInputStream());
			out = new BufferedOutputStream(socket.getOutputStream());
			
			out.write(TCPUtil.getCommandBytes(TCPUtil.Command.GET_LAST_TRAINED));
			byte [] moduleBytes = TCPUtil.getModuleNameBytes(moduleName);
			out.write(TCPUtil.getPayloadLengthBytes(moduleBytes.length));
			out.write(moduleBytes);
			out.flush();
			
			int status = TCPUtil.getIntStatus(in);
			long payloadLength = TCPUtil.getLongPayloadLength(in);
			
			logger.log(Level.INFO, "Status {0}", status);
			logger.log(Level.INFO, "Payload Length {0}", payloadLength);
			
			return new Long(TCPUtil.getPayload(in, payloadLength));
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				
				in.close();
				out.close();
				socket.close();
			} catch(IOException io) {
				io.printStackTrace();
			}
		}
		return 0L;
	}
	
	private long train(String moduleName, String filePath, int trainCommand) throws Exception {
		
		Socket socket = null;
		BufferedInputStream in = null;
		BufferedOutputStream out = null;
		
		String fileName = new File(filePath).getName();
		
		try {
			
			socket = new Socket(host, port);
			in = new BufferedInputStream(socket.getInputStream());
			out = new BufferedOutputStream(socket.getOutputStream());
			
			out.write(TCPUtil.getCommandBytes(trainCommand));
			byte [] fileNameBytes = TCPUtil.getFileNameBytes(fileName);
			byte [] moduleNameBytes = TCPUtil.getModuleNameBytes(moduleName);
			out.write(TCPUtil.getPayloadLengthBytes(fileNameBytes.length + moduleNameBytes.length));
			out.write(fileNameBytes);
			out.write(moduleNameBytes);
			out.flush();
			
			int status = TCPUtil.getIntStatus(in);
			long payloadLength = TCPUtil.getLongPayloadLength(in);
			
			logger.log(Level.INFO, "Status {0}", status);
			logger.log(Level.INFO, "Payload Length {0}", payloadLength);
			
			if(payloadLength > 0) {
				return Long.valueOf(TCPUtil.getPayload(in, payloadLength));
			}
			return 0L;
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				
				in.close();
				out.close();
				socket.close();
			} catch(IOException io) {
				io.printStackTrace();
			}
		}
		return 0L;
	}
	
	private boolean sendFile(String filePath) throws IOException, Exception {

		File zipFile = createZipFile(filePath);
		long fileSize = zipFile.length();
		FileInputStream in = null;
		int chunk = 0;
		boolean success = true;
		String fileName = new File(filePath).getName();
		
		try {
			
			in = new FileInputStream(zipFile);
			
			while(fileSize > 0) {

				Socket socket = null;
				BufferedOutputStream out = null;
				BufferedInputStream sockIn = null;
				try {
					
					socket = new Socket(host, port);
					out = new BufferedOutputStream(socket.getOutputStream());
					sockIn = new BufferedInputStream(socket.getInputStream());
				
					out.write(TCPUtil.getCommandBytes(TCPUtil.Command.TRANSMIT_FILE));
					long payloadLength = TCPUtil.HeaderLengths.FILE_NAME_LENGTH + TCPUtil.HeaderLengths.SEQUENCE_LENGTH;
					if(fileSize > TCPUtil.CHUNK_SIZE) {
						payloadLength += TCPUtil.CHUNK_SIZE;
					}
					else {
						payloadLength += fileSize;
 				}
					
					out.write(TCPUtil.getPayloadLengthBytes(payloadLength));
					out.write(TCPUtil.getSequenceBytes(chunk));
					out.write(TCPUtil.getFileNameBytes(fileName));

					byte[] buffer = new byte[TCPUtil.BUFFER_LENGTH];
					File tempChunkFile = new File("temp" + File.separator + fileName + ".tempchunk");
					File tmpParent = tempChunkFile.getParentFile();
					if(!tmpParent.exists()) {
						tmpParent.mkdirs();
					}
					
					FileOutputStream tempOut = new FileOutputStream(tempChunkFile);
					
					int len;
					while((len = in.read(buffer)) > 0) {

						tempOut.write(buffer, 0, len);
						out.write(buffer, 0, len);
					}
					tempOut.flush();
					out.flush();
					tempOut.close();
				
					int status = TCPUtil.getIntStatus(sockIn);
					logger.log(Level.INFO, "Status {0}", status);
				
					int retryCount = 0;
					while(status == TCPUtil.Status.SERVER_ERROR && retryCount < TCPUtil.RETRY_COUNT) {

						out.close();
						sockIn.close();
						socket.close();
						
						socket = new Socket(host, port);
						out = new BufferedOutputStream(socket.getOutputStream());
						sockIn = new BufferedInputStream(socket.getInputStream());
						
						FileInputStream fin = new FileInputStream(tempChunkFile);
					
						out.write(TCPUtil.getCommandBytes(TCPUtil.Command.TRANSMIT_FILE));
						out.write(TCPUtil.getPayloadLengthBytes(payloadLength));
						out.write(TCPUtil.getSequenceBytes(chunk));
						out.write(TCPUtil.getFileNameBytes(fileName));

						while((len = fin.read(buffer)) > 0) {
							out.write(buffer, 0, len);
						}
						out.flush();
						fin.close();
						status = TCPUtil.getIntStatus(sockIn);
						retryCount++;
						logger.log(Level.INFO, "Retry {0}: Status {1}", new String[]{String.valueOf(retryCount), String.valueOf(status)});
					}
					tempChunkFile.delete();
					success &= status == TCPUtil.Status.OK;
					fileSize -= (payloadLength - (TCPUtil.HeaderLengths.FILE_NAME_LENGTH + TCPUtil.HeaderLengths.SEQUENCE_LENGTH));
				} catch(Exception e) {
					e.printStackTrace();
				} finally {
					out.close();
					sockIn.close();
					socket.close();
					zipFile.delete();
				}
			}
		} finally {
			in.close();
		}
		return success;
	}
	
	private boolean mergeFiles(String filePath) {
		
		Socket socket = null;
		BufferedInputStream in = null;
		BufferedOutputStream out = null;
		String fileName = new File(filePath).getName();
		
		try {
			
			socket = new Socket(host, port);
			in = new BufferedInputStream(socket.getInputStream());
			out = new BufferedOutputStream(socket.getOutputStream());
			byte [] fileNameBytes = fileName.getBytes();
			out.write(TCPUtil.getCommandBytes(TCPUtil.Command.MERGE_FILES));
			out.write(TCPUtil.getPayloadLengthBytes(fileNameBytes.length));
			out.write(fileNameBytes);
			out.flush();
			
			return TCPUtil.Status.OK == TCPUtil.getIntStatus(in);
			
		} catch(IOException io) {
			io.printStackTrace();
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				
				in.close();
				out.close();
				socket.close();
			} catch(IOException io) {
				io.printStackTrace();
			}
		}
		return false;
	}
	
	private File createZipFile(String filePath) {
		
		FileInputStream in = null;
		ZipOutputStream zos = null;
		ZipEntry ze = null;
		File zipFile = null;
		byte[] buffer = new byte[TCPUtil.BUFFER_LENGTH];
		String fileName = new File(filePath).getName();

		try {

			zipFile = new File("temp" + File.separator + fileName + ".zip");
			logger.log(Level.INFO, "Creating zip file {0}", zipFile);
			if(!zipFile.getParentFile().exists()) {
				zipFile.getParentFile().mkdirs();
			}
			
			FileOutputStream fos = new FileOutputStream(zipFile);
			zos = new ZipOutputStream(fos);
			ze = new ZipEntry(fileName);
			zos.putNextEntry(ze);
			in = new FileInputStream(filePath);

			int len;
			while((len = in.read(buffer)) > 0) {
				zos.write(buffer, 0, len);
			}
			logger.log(Level.INFO, "Zip file created");

		} catch(IOException ex) {
			ex.printStackTrace();
		} finally {
			try {
			in.close();
			zos.closeEntry();
			zos.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		return zipFile;
	}
	
	public boolean sendFeedback(String moduleName, FeedbackData data) throws Exception {
		
		ObjectOutputStream oos = null;
		Socket socket = null;
		BufferedInputStream in = null;
		BufferedOutputStream out = null;
		FileInputStream fin = null;
		File feedbackFile = null;
		
		try {
			
			feedbackFile = new File("temp" + File.separator + moduleName + ".airlfeedback");
			oos = new ObjectOutputStream(new FileOutputStream(feedbackFile));
			oos.writeObject(data);
			oos.flush();
			oos.close();
			fin = new FileInputStream(feedbackFile);
			
			socket = new Socket(host, port);
			out = new BufferedOutputStream(socket.getOutputStream());
			in = new BufferedInputStream(socket.getInputStream());
		
			out.write(TCPUtil.getCommandBytes(TCPUtil.Command.TRANSMIT_FEEDBACK));
			byte [] moduleBytes = TCPUtil.getModuleNameBytes(moduleName);
			long payloadLength = feedbackFile.length() + TCPUtil.HeaderLengths.MODULE_NAME_LENGTH;
			out.write(TCPUtil.getPayloadLengthBytes(payloadLength));
			out.write(moduleBytes);
			
			byte[] buffer = new byte[TCPUtil.BUFFER_LENGTH];
			int len = 0;
			while((len = fin.read(buffer)) > 0) {
				out.write(buffer, 0, len);
			}
			out.flush();
			fin.close();
			
			return TCPUtil.Status.OK == TCPUtil.getIntStatus(in);
			
		} catch(IOException io) {
			io.printStackTrace();
		} finally {
			try {
				
				in.close();
				out.close();
				socket.close();
				feedbackFile.delete();
			} catch(IOException io) {
				io.printStackTrace();
			}
		}
		return false;
	}
	
	private String host = null;
	private int port = 0;
	private Logger logger = Logger.getLogger(com.me.airlab.client.ClientClassifier.class.getName());
}
