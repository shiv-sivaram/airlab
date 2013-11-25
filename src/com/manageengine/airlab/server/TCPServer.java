/**
 * 
 */
package com.manageengine.airlab.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.ServerSocket;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.manageengine.airlab.StatusData;
import com.manageengine.airlab.TCPUtil;

/**
 * @author Sivanand
 * 
 */
public class TCPServer {

	public static TCPServer getInstance() {
		return tcpServer ;
	}
	
	private TCPServer() {

	}

	public void init() {

		try {
			server = new ServerSocket(Integer.parseInt(TCPUtil.getServerProperty("PORT")));
		} catch(IOException io) {
			logger.log(Level.SEVERE, io.toString());
		}
	}
	
	public void startServer() {

		while(true) {

			try {

				logger.log(Level.INFO, "Waiting for client");
				Socket clientSocket = server.accept();
				new TCPServer.ClientConnection(clientSocket);
				logger.log(Level.INFO, "Client connected: {0}", clientSocket);
			} catch(IOException io) {
				logger.log(Level.SEVERE, io.toString());
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		TCPServer server = getInstance();
		server.init();
		server.startServer();
	}

	private static TCPServer tcpServer = new TCPServer();
	private ServerSocket server = null;
	private Logger logger = Logger.getLogger(com.manageengine.airlab.server.TCPServer.class.getName());

	class ClientConnection extends Thread {

		public ClientConnection(Socket client) {

			this.client = client;
			this.start();
		}

		public void run() {

			long threadId = this.getId();
			
			logger.log(Level.INFO, "["+threadId+"] Client from {0} connected", client.getInetAddress());
			
			BufferedInputStream in = null;
			BufferedOutputStream out = null;
			int command = 0;
			long payloadLength = 0l;
			File tmpFile = null;
			FileOutputStream fileOut = null;

			try {

				in = new BufferedInputStream(client.getInputStream());
				out = new BufferedOutputStream(client.getOutputStream());
				
				command = TCPUtil.getIntCommand(in);
				payloadLength = TCPUtil.getLongPayloadLength(in);
				
				logger.log(Level.INFO, "["+threadId+"] Command {0}", command);
				logger.log(Level.INFO, "["+threadId+"] Payload Length {0}", payloadLength);

				if(payloadLength > 0L) {
					
					File tmpFolder = new File("tmp");
					if(!tmpFolder.exists()) {
						tmpFolder.mkdir();
					}
					
					tmpFile = File.createTempFile("AIRLab", ".airltemp", tmpFolder);
					fileOut = new FileOutputStream(tmpFile);
					byte[] buffer = new byte[TCPUtil.BUFFER_LENGTH];
					int len = 0;
					
					logger.log(Level.INFO, "["+threadId+"] Going to read payload");
					
					while(payloadLength > 0L) {
						
						len = in.read(buffer);
						fileOut.write(buffer, 0, len);
						payloadLength -= len;
					}
					
					fileOut.flush();
					logger.log(Level.INFO, "["+threadId+"] Wrote temporary file {0}", tmpFile.getPath());
				}
			} catch(IOException io) {
				io.printStackTrace();
			} finally {
				
				try {
					
					if(fileOut != null) {
						fileOut.close();
					}
				} catch(IOException io) {
					io.printStackTrace();
				}
			}
			
			try {

				String[] temp = TCPUtil.getCommand(String.valueOf(command)).split(";");
				if(temp == null || temp.length != 2) {
					throw new NoSuchMethodError(String.valueOf(command));
				}

				logger.log(Level.INFO, "["+threadId+"] Class {0}", temp[0]);
				logger.log(Level.INFO, "["+threadId+"] Method {0}", temp[1]);

				Class<?> commandClass = Class.forName(temp[0]);
				Object commandObj = commandClass.newInstance();

				Class[] params = new Class[1];
				params[0] = File.class;
				Method commandMethod = commandClass.getDeclaredMethod(temp[1], params);
				Object[] args = new Object[1];
				args[0] = tmpFile;

				StatusData returnValue = (StatusData)commandMethod.invoke(commandObj, args);
				logger.log(Level.INFO, "["+threadId+"] Return Value {0}", returnValue);
				
				out.write(TCPUtil.getStatusBytes(returnValue.getStatusCode()));
				byte [] message = returnValue.getMessage().getBytes();
				out.write(TCPUtil.getPayloadLengthBytes(message.length));
				out.write(message);
				out.flush();
				
				if(tmpFile != null && tmpFile.exists()) {
					tmpFile.delete();
				}
			} catch(Exception e) {
				
				try {
					
					out.write(TCPUtil.getStatusBytes(TCPUtil.Status.SERVER_ERROR));
					out.write(TCPUtil.getPayloadLengthBytes(0));
					out.flush();
				} catch(Exception x) {
					x.printStackTrace();
				}
				e.printStackTrace();
			} finally {
				
				try {

					in.close();
					out.close();
					client.close();
					tmpFile.delete();
					logger.log(Level.INFO, "["+threadId+"] Connection completed");
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}

		private Socket client = null;
	}
}
