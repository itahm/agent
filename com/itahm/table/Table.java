package com.itahm.table;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.json.JSONFile;

public class Table implements Closeable {
	
	private final static File dataRoot = ITAhM.getRoot();
	protected JSONFile file;
	protected JSONObject table;
	protected int sequence = 0;
	protected int commit = 0;
	
	protected void load(String tableName) throws IOException {
		this.file = new JSONFile((new File(dataRoot, tableName)));
		this.table = file.getJSONObject();
	}
	
	protected boolean isEmpty() {
		return this.table.length() == 0;
	}
	
	public JSONObject getJSONObject() {
		return this.table;
	}
	
	public JSONObject getJSONObject(String key) {
		if (this.table.has(key)) {
			return this.table.getJSONObject(key);
		}
		
		return null;
	}
	
	public int lock() {
		return this.sequence++;
	}
	
	public boolean commit(int sequence) {
		if (sequence - this.commit >= 0) {
			this.commit = this.sequence;
			
			return true;
		}
		
		return false;
	}
	
	public void save() {
		try {
			this.file.save();
		} catch (IOException e) {
			// fatal error
			e.printStackTrace();
		}
	}

	public void save(JSONObject data) {
		try {
			this.file.save(data);
			
			this.table = data;
		} catch (IOException e) {
			// fatal error
			e.printStackTrace();
		}
	}
	
	@Override
	public void close() throws IOException {
		this.file.close();
	}
}
