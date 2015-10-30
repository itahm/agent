package com.itahm;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;

public class Logger implements Closeable, Runnable {

	private LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();
	private BufferedWriter file = null;
	private SimpleDateFormat fileFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private SimpleDateFormat fileNameFormat = new SimpleDateFormat("yyyyMMdd");
	private String fileName = new String("");
	private String logRoot = new String("");
	
	public Logger(String logRoot) {
		File dir = new File(this.logRoot = logRoot);
		
		if (!dir.exists() || !dir.isDirectory()) {
		    dir.mkdir();
		}
		
		new Thread(this).start();
	}
	
	public boolean log(String log) {System.out.println(log);
		return queue.add(log);
	}
	
	private void write(String log) throws IOException {
		Date curTime = Calendar.getInstance().getTime();
		String fileName = fileNameFormat.format(curTime);
		
		if(!fileName.equals(this.fileName)) {
			close();
			
			file = new BufferedWriter(new FileWriter(logRoot + File.separator + (this.fileName = fileName) +".log", true));
		}
		
		this.file.write("["+ fileFormat.format(curTime) +"]\t");
		this.file.write(log);
		this.file.newLine();
		this.file.flush();
	}
	
	@Override
	public void close() {
		if(file != null) {
			try {
				file.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			fileName = new String("");
			file = null;
		}
	}

	@Override
	public void run() {
		
		try {
			while(true) {
				try {
					write(queue.take());
				}
				catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
		
	}
}
