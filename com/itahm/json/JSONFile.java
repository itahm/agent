package com.itahm.json;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

/**
 * The Class JSONFile.
 */
public class JSONFile implements Closeable{
	
	protected JSONObject json;
	private final RandomAccessFile file;
	private FileChannel channel;
	
	public JSONFile(File f) throws IOException {
		file = new RandomAccessFile(f, "rws");
		channel = file.getChannel();
		
		try {
			json = getJSONObject(this.channel);
			
			if (json == null) {
				json = new JSONObject();
				
				save();
			}
		} catch (IOException ioe) {
			file.close();
			
			throw ioe;
		}
	}
	
	private static JSONObject getJSONObject(FileChannel fc) throws IOException {
		long size = fc.size();
		
		if (size != (int)size) {
			throw new IOException("file size " +size);
		}
		
		if (size <= 0) {
			return null;
		}
		
		ByteBuffer buffer = ByteBuffer.allocate((int)size);
		
		while (size > 0) {
			size -= fc.read(buffer);
		}
		
		buffer.flip();
		
		try {
			return new JSONObject(StandardCharsets.UTF_8.decode(buffer).toString());
		}
		catch (JSONException jsone) {
			throw new IOException(jsone);
		}
	}
	
	public static JSONObject getJSONObject(File file) throws IOException {
		if (!file.isFile()) {
			return null;
		}
		
		try (
			RandomAccessFile raf = new RandomAccessFile(file, "r");
		) {
			return getJSONObject(raf.getChannel());
		}
		catch (FileNotFoundException fnfe) {
		}
		
		return null;
	}
	
	public JSONObject getJSONObject() {
		return this.json;
	}
	
	public Object get(String key) {
		return this.json.has(key)? this.json.get(key): null;
	}
	
	public void save() throws IOException {	
		ByteBuffer buffer = ByteBuffer.wrap(this.json.toString().getBytes(StandardCharsets.UTF_8.name()));
		
		this.file.setLength(0);
		this.channel.write(buffer);
	}
	
	public void save(JSONObject json) throws IOException {	
		this.json = json;
		
		save();
	}
	
	@Override
	public void close() throws IOException {
		this.file.close();
	}
	
}