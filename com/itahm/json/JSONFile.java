package com.itahm.json;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.ITAhMException;

// TODO: Auto-generated Javadoc
/**
 * The Class JSONFile.
 */
public class JSONFile implements Closeable{
	
	/** The json. */
	protected JSONObject json;
	
	/** The file. */
	private RandomAccessFile file = null;
	
	/** The channel. */
	private FileChannel channel;
	
	/**
	 * Instantiates a new JSON file.
	 */
	public JSONFile() {
	}
	
	public JSONFile(File file) throws IOException {
		load(file);
	}
	
	/**
	 * Load.
	 *
	 * @param file the file
	 * @return the JSON file
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws ITAhMException the IT ah m exception
	 * @throws FileNotFoundException 
	 */
	public JSONFile load(File file) throws IOException {
		if (this.file != null) {
			close();
		}
		
		this.file = new RandomAccessFile(file, "rws");
		this.channel = this.file.getChannel();
		
		try {
			long size = this.channel.size();
			if (size != (int)size) {
				System.out.println("fatal error: too long file size.");
			}
			else if (size > 0) {
				ByteBuffer buffer = ByteBuffer.allocate((int)size);
				
				this.channel.read(buffer);
				buffer.flip();
				try {
					this.json = new JSONObject(Charset.defaultCharset().decode(buffer).toString());
				}
				catch (JSONException jsone) {
					System.out.println("fatal error: invalid json file "+ file.getName() +".");
				}
			}
			else {
				this.json = new JSONObject();
				
				save();
			}
		} catch (IOException ioe) {
			this.file.close();
			
			throw ioe;
		}
		
		return this;
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
					return new JSONObject(Charset.defaultCharset().decode(bb).toString());
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
	 * @return the JSON object. null json 형식이 아니라던가 어떤 문제가 생겼을때
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
		ByteBuffer buffer = ByteBuffer.wrap(this.json.toString().getBytes());
		
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
		if (this.channel != null) {
			this.channel.close();
		}
		
		if (this.file != null) {
			this.file.close();
		}
	}
	
}