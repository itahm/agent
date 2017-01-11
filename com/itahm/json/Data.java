package com.itahm.json;

import java.io.File;
import java.util.Calendar;

import com.itahm.json.JSONObject;

public abstract class Data {

	protected final JSONObject data;
	
	private final File root;
	private Calendar date;
	private long end;
	
	public Data(File f) {
		root = f;
		data = new JSONObject();
		date = Calendar.getInstance();
	}
	
	public void buildJSON() {
		long mills = this.date.getTimeInMillis();
		
		if (this.end <= mills) {
			return;
		}
		
		buildNext(new File(this.root, Long.toString(mills)));
		
		this.date.add(Calendar.DATE, 1);
		
		buildJSON();
	}
	
	public JSONObject getJSON(long startMills, long endMills) {
		this.data.clear();
		
		this.end = endMills;
		
		this.date.setTimeInMillis(startMills);
		this.date.set(Calendar.MILLISECOND, 0);
		this.date.set(Calendar.SECOND, 0);
		this.date.set(Calendar.MINUTE, 0);
		this.date.set(Calendar.HOUR_OF_DAY, 0);
		
		buildJSON();
		
		return this.data;
	}
	
	abstract protected void buildNext(File dir);
}
