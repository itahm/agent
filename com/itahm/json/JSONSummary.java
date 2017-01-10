package com.itahm.json;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import com.itahm.json.JSONException;
import com.itahm.json.JSONObject;

public class JSONSummary implements Data {
	
	/** rollingRoot, itahm/snmp/ip address/resource/index */
	private final File root;
	
	private Calendar date;
	private long end;
	private long nextDay;
	private JSONObject data = null;
	
	public JSONSummary(File rollingRoot) {
		this.root = rollingRoot;
	}
	
	public JSONObject getJSON(JSONObject json) {
		long date = this.date.getTimeInMillis();
		String key = Long.toString(date);
		
		if (this.end < date) {
			return json;
		}
		
		if (date >= this.nextDay) {
			nextDate();
		}
		
		try {
			if (this.data != null && this.data.has(key)) {
				json.put(key, this.data.getJSONObject(key));
			}
		}
		catch (JSONException jsone) {
		}
		
		this.date.add(Calendar.HOUR_OF_DAY, 1);
		
		return getJSON(json);
	}
	
	private void nextDate() {
		Calendar calendar = Calendar.getInstance();
		long day = this.nextDay;
		
		calendar.setTimeInMillis(this.nextDay);
		calendar.add(Calendar.DATE, 1);
		
		this.nextDay = calendar.getTimeInMillis();
		
		try {
			File dir = new File(this.root, Long.toString(day));
			File file = new File(dir, "summary");
			
			this.data = JSONFile.getJSONObject(file);
		} catch (IOException e) {
			this.data = null;
			
			this.date.setTimeInMillis(this.nextDay);
		}
	}

	@Override
	public JSONObject getJSON(long startMills, long endMills) {
		Calendar calendar = Calendar.getInstance();
		
		this.date = Calendar.getInstance();
		
		this.date.setTimeInMillis(startMills);
		this.end = endMills;
		
		this.date.set(Calendar.MILLISECOND, 0);
		this.date.set(Calendar.SECOND, 0);
		this.date.set(Calendar.MINUTE, 0);
		
		calendar.setTimeInMillis(startMills);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		
		nextDay = calendar.getTimeInMillis();
		
		nextDate();
		
		return getJSON(new JSONObject());
	}
	
}
