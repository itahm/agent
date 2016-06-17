package com.itahm;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;

import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.util.DailyFile;

public class Log implements Closeable {

	private final Set<Request> waiter;
	
	private DailyFile dailyFile;
	private RandomAccessFile indexFile;
	private JSONObject indexObject;
	private FileChannel indexChannel;
	private long index;
	private final JSONObject log;
	
	public Log(File dataRoot) throws IOException {
		this.waiter = new HashSet<Request> ();
		
		File logRoot = new File(dataRoot, "log");
		File indexFile = new File(logRoot, "index");
		
		logRoot.mkdir();
		
		this.dailyFile = new DailyFile(logRoot);
		
		byte [] bytes = this.dailyFile.read(DailyFile.trim(Calendar.getInstance()).getTimeInMillis());
		
		if (bytes == null) {
			this.log = new JSONObject();
		}
		else {
			this.log = new JSONObject(new String(bytes));
			System.out.println(this.log);
		}
		
		this.indexFile = new RandomAccessFile(indexFile, "rws");
		this.indexChannel = this.indexFile.getChannel();
		
		loadIndex();
	}
	
	private void loadIndex() throws IOException {
		int size = (int)this.indexChannel.size();
		
		if (size == 0) {
			this.indexObject = new JSONObject();
			
			this.indexObject.put("index", this.index = 0);
		}
		else {
			ByteBuffer buffer = ByteBuffer.allocate(size);
			byte[] bytes = new byte [size];
			
			this.indexChannel.read(buffer);

			buffer.flip();
			
			buffer.get(bytes);
			
			this.indexObject = new JSONObject(new String(bytes));
			this.index = this.indexObject.getLong("index");
		}
		
		System.out.println("Log index ready: "+ this.index);
	}
	
	private synchronized long getIndex() {
		long index = this.index++;
		
		this.indexObject.put("index", this.index);
		
		try {
			this.indexFile.setLength(0);
			
			this.indexChannel.write(ByteBuffer.wrap(this.indexObject.toString().getBytes()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return index; 
	}
	
	public void write(String ip, boolean custom, boolean status, String message) throws IOException {
		JSONObject logData = new JSONObject();
		long index = getIndex();
		
		logData
			.put("index", index)
			.put("ip", ip)
			.put("custom", custom)
			.put("status", status)
			.put("message", message)
			.put("date", Calendar.getInstance().getTimeInMillis());
		
		if (this.dailyFile.roll()) {
			this.log.clear();
		}
		
		this.log.put(Long.toString(index), logData);
		
		this.dailyFile.write(this.log.toString().getBytes());
		
		dispatch(logData);
	}

	public String read(long mills) throws IOException {
		byte [] bytes = this.dailyFile.read(mills);
		
		if (bytes != null) {
			return new String(bytes);
		}
		
		return new JSONObject().toString();
	}
	
	public JSONObject getDailyLog(long index) {
		String key = Long.toString(index);
		
		if (this.log.has(key)) {
			return this.log.getJSONObject(key);
		}
		
		return null;
	}
	
	/**
	 * waiter가 원하는 이벤트 있으면 돌려주고 없으면 waiter 큐에 추가  
	 * @param waiter
	 * @throws IOException 
	 */
	public void listen(Request request, long l) throws IOException {
		String index = Long.toString(l);
		
		synchronized(this.log) {
			if (this.log.has(index)) {
				request.sendResponse(Response.getInstance(200, "OK", this.log.getJSONObject(index)));
			}
			else {
				this.waiter.add(request);
			}
		}
	}
	
	public void cancel(Request request) {
		synchronized(this.waiter) {
			this.waiter.remove(request);
		}
	}
	
	private void dispatch(JSONObject logData) {
		synchronized(this.waiter) {
			for (Request request : this.waiter) {
				try {
					request.sendResponse(Response.getInstance(200, "OK", logData));
				} catch (IOException e) {
					// TODO 이 경우 소켓 어떻게 닫아주나?
					e.printStackTrace();
				}
			}
			
			this.waiter.clear();
		}
		
		ITAhM.gcmm.broadcast(logData.getString("message"));
	}
	
	public void close() {
		try {
			this.dailyFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			this.indexChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			this.indexFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
