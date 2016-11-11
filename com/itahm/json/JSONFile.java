package com.itahm.json;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

import org.json.JSONException;
import org.json.JSONObject;

// TODO: Auto-generated Javadoc
/**
 * The Class JSONFile.
 */
public class JSONFile implements Closeable{
	
	/** The json. */
	protected JSONObject json;
	
	/** The file. */
	private final RandomAccessFile file;
	
	/** The channel. */
	private FileChannel channel;
	
	/**
	 * Instantiates a new JSON file.
	 */
	
	public JSONFile(File f) throws IOException {
		file = new RandomAccessFile(f, "rws");
		channel = file.getChannel();
		
		try {
			load();
		} catch (IOException ioe) {
			file.close();
			
			throw ioe;
		}
	}

	private void load() throws IOException {
		long size = this.channel.size();
		
		if (size != (int)size) {
			throw new IOException("custom ITAhM exception: file size.");
		}
		
		if (size > 0) {
			ByteBuffer buffer = ByteBuffer.allocate((int)size);
			
			this.channel.read(buffer);
			buffer.flip();
			try {
				this.json = new JSONObject(StandardCharsets.UTF_8.decode(buffer).toString());
			}
			catch (JSONException jsone) {
				throw new IOException("custom ITAhM exception: invalid json file.");
			}
		}
		else {
			this.json = new JSONObject();
			
			save();
		}
	}
	
	/**
	 * Gets the JSON object.
	 *
	 * @param file the file
	 * @return the JSON object. return null if file size is more than Integer.MAX_VALUE, or invalid json format, or empty file.
	 * @throws IOException 
	 */
	public static JSONObject getJSONObject(File file) throws IOException {
		if (!file.isFile()) {
			return null;
		}
		
		try (
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			FileChannel fc = raf.getChannel();
		) {
			long size = fc.size();
			if (size != (int)size) {				
				return null;
			}
			else if (size > 0) {
				ByteBuffer bb = ByteBuffer.allocate((int)size);
				fc.read(bb);
				
				bb.flip();
			
				try {
					return new JSONObject(StandardCharsets.UTF_8.decode(bb).toString());
				}
				catch (JSONException jsone) {
				}
			}
		}
		catch (FileNotFoundException fnfe) {
		}
		
		return null;
	}
	
	/**
	 * Gets the JSON object.
	 *
	 * @return the JSON object.
	 */
	public JSONObject getJSONObject() {
		return this.json;
	}
	
	/**
	 * Save.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void save() throws IOException {	
		ByteBuffer buffer = ByteBuffer.wrap(this.json.toString().getBytes(StandardCharsets.UTF_8.name()));
		
		this.file.setLength(0);
		this.channel.write(buffer);
	}
	
	/**
	 * Save.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void save(JSONObject json) throws IOException {	
		this.json = json;
		
		save();
	}
	
	/* (non-Javadoc)
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		this.file.close();
	}
	
}