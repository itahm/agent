package com.itahm.json;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import com.itahm.json.JSONObject;

public class JSONSummary implements Data {
	
	private final File root;
	private long end;
	
	public JSONSummary(File rollingRoot) {
		root = rollingRoot;
	}
	
	public JSONObject getJSON(JSONObject json, Calendar date) {
		JSONObject data;
		
		if (this.end < date.getTimeInMillis()) {
			return json;
		}
		
		data = getNextData(date);
		
		if (data != null) {
			for (Object key : data.keySet()) {
				json.put((String)key, data.getJSONObject((String)key));
			}
		}
		
		date.add(Calendar.DATE, 1);
		
		return getJSON(json, date);
	}
	
	private JSONObject getNextData(Calendar date) {
		File dir = new File(this.root, Long.toString(date.getTimeInMillis()));
		File file = new File(dir, "summary");
		
		try {	
			return JSONFile.getJSONObject(file);
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public JSONObject getJSON(long startMills, long endMills) {
		Calendar date = Calendar.getInstance();
		
		date.setTimeInMillis(startMills);
		
		this.end = endMills;
		
		date.set(Calendar.MILLISECOND, 0);
		date.set(Calendar.SECOND, 0);
		date.set(Calendar.MINUTE, 0);
		
		return getJSON(new JSONObject(), date);
	}
	
}
