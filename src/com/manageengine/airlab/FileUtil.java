/**
 * 
 */
package com.manageengine.airlab;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Sivanand
 *
 */
public class FileUtil {
	
	public StatusData transmitFile(File payload) throws Exception {
		
		FileInputStream in = new FileInputStream(payload);
		BufferedInputStream bin = new BufferedInputStream(in);
		int sequence = TCPUtil.getIntSequence(bin);
		String fileName = TCPUtil.getStringFileName(bin);

		FileOutputStream out = new FileOutputStream("tmp" + File.separator + fileName + ".airlchunk" + sequence);
		
		int len = 0;
		byte[] buffer = new byte[TCPUtil.BUFFER_LENGTH];
		while((len = bin.read(buffer)) > 0) {
			out.write(buffer, 0, len);
		}
		out.flush();
		out.close();
		bin.close();
		
		logger.log(Level.INFO, "Created chunk {0} for file {1}. Temp file {2}", new String[]{String.valueOf(sequence), fileName, String.valueOf(new File("tmp" + File.separator + fileName + ".airlchunk" + sequence).exists())});
		return new StatusData(TCPUtil.Status.OK, "Received "+fileName+" part "+sequence);
		
	}
	
	public StatusData mergeFiles(File payload) throws Exception {
		
		FileInputStream in = new FileInputStream(payload);
		BufferedInputStream bin = new BufferedInputStream(in);
		File file = new File(TCPUtil.getStringFileName(bin));
		final String fileName = file.getName();
		
		if(!new File("tmp" + File.separator + fileName + ".airlchunk0").exists()) {
			return new StatusData(TCPUtil.Status.NO_FILES, "Nothing to merge");
		}
		
		File outputFile = new File("tmp" + File.separator + fileName + ".zip");
		FileOutputStream fout = new FileOutputStream(outputFile);
		File [] toMergeFiles = new File("tmp").listFiles(new FilenameFilter() {
			
			public boolean accept(File dir, String name) {
				boolean b = name.matches(fileName+".airlchunk[0-9]+");
				System.out.println(b);
				return b;
			}
		});
		
		for(int i=0;i<toMergeFiles.length;i++) {
			
			File toProcess = new File("tmp" + File.separator + fileName + ".airlchunk" + i);
			FileInputStream fin = new FileInputStream(toProcess);
			int len = 0;
			byte [] buffer = new byte[TCPUtil.BUFFER_LENGTH];
			while((len = fin.read(buffer)) > 0) {
				fout.write(buffer, 0, len);
			}
			fin.close();
			toProcess.delete();
		}
		in.close();
		fout.close();
		
		logger.log(Level.INFO, "Merged and created zip file exists {0}", outputFile.exists());
		
		String unzippedFile = unzipFile(outputFile);
		return new StatusData(TCPUtil.Status.OK, "Merged file "+unzippedFile);
	}
	
	private String unzipFile(File zipFile) {

		String fileName = null;
		try {

			ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
			ZipEntry ze = zis.getNextEntry();
			byte[] buffer = new byte[TCPUtil.BUFFER_LENGTH];
			int len = 0;
			

			if(ze != null) {

				fileName = ze.getName();
				fileName = "tmp" + File.separator + fileName;
				File unzippedFile = new File(fileName);
				FileOutputStream fos = new FileOutputStream(unzippedFile);

				while((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}

				fos.close();
				ze = zis.getNextEntry();
			}

			zis.closeEntry();
			zis.close();
			zipFile.delete();
			} catch(IOException io) {
				io.printStackTrace();
			}
		return fileName;
	}
	
	private Logger logger = Logger.getLogger(com.manageengine.airlab.FileUtil.class.getName());
}
