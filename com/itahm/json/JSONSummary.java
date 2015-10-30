package com.itahm.json;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONSummary implements Data {
	
	/** rollingRoot, itahm/snmp/ip address/resource/index */
	private final File root;
	
	private Calendar date;
	private final long end;
	private long nextDay;
	private JSONObject data = null;
	private final JSONObject result;
	
	public JSONSummary(JSONObject json, File rollingRoot, long startDate, long endDate) {
		Calendar calendar = Calendar.getInstance();
		
		date = Calendar.getInstance();
		
		result = json;
		root = rollingRoot;
		date.setTimeInMillis(startDate);
		end = endDate;
		
		date.set(Calendar.MILLISECOND, 0);
		date.set(Calendar.SECOND, 0);
		date.set(Calendar.MINUTE, 0);
		
		calendar.setTimeInMillis(startDate);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		
		nextDay = calendar.getTimeInMillis();
		
		nextDate();
	}
	
	public boolean next() {
		if (this.end < this.date.getTimeInMillis()) {
			return false;
		}
		
		long date = this.date.getTimeInMillis();
		String key = Long.toString(date);
		
		if (date >= this.nextDay) {
			nextDate();
		}
		
		try {
			if (this.data != null && this.data.has(key)) {
				this.result.put(key, this.data.getJSONObject(key));
			}
		}
		catch (JSONException jsone) {
		}
		
		this.date.add(Calendar.HOUR_OF_DAY, 1);
		
		return true;
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
	
}
