package com.itahm.table;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.json.JSONFile;

public class Table implements Closeable {
	
	private final static File dataRoot = ITAhM.getRoot();
	protected JSONFile file = new JSONFile();
	protected JSONObject table;
	
	protected void load(String tableName) throws IOException {
		this.file.load(new File(dataRoot, tableName));
		this.table = file.getJSONObject();
	}
	
	protected boolean isEmpty() {
		return this.table.length() == 0;
	}
	
	public JSONObject getJSONObject() {
		return this.table;
	}
	
	protected void save() throws IOException {
		this.file.save();
	}

	public void save(JSONObject data) throws IOException {
		this.file.save(data);
	}
	
	@Override
	public void close() throws IOException {
		this.file.close();
	}
}
