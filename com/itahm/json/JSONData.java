package com.itahm.json;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONData implements Data{

	/** rollingRoot, itahm/snmp/ip address/resource/index */
	private final File root;
	
	private Calendar date;
	private long end;
	private long nextHourMills;
	private JSONObject data = null;
	
	public JSONData(File rollingRoot) {
		this.root = rollingRoot;
	}
	
	public JSONObject getJSON(JSONObject json) {
		long date = this.date.getTimeInMillis();
		String key = Long.toString(date);
		
		if (this.end < date) {
			return json;
		}
		
		if (date >= this.nextHourMills) {
			nextDate();
		}
		
		try {
			if (this.data != null && this.data.has(key)) {
				json.put(key, this.data.getLong(key));
			}
		}
		catch (JSONException jsone) {
		}
		
		this.date.add(Calendar.MINUTE, 1);
		
		return getJSON(json);
	}
	
	/** next hour or next day*/
	private void nextDate() {
		Calendar calendar = Calendar.getInstance();
		long hourMills = this.nextHourMills;
		File directory;
		
		// 날짜 directory 계산
		calendar.setTimeInMillis(hourMills);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		
		directory = new File(this.root, Long.toString(calendar.getTimeInMillis()));
		
		// nextHourMills 계산
		calendar.setTimeInMillis(hourMills);
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		
		this.nextHourMills = calendar.getTimeInMillis();
		
		try {
			this.data = JSONFile.getJSONObject(new File(directory, Long.toString(hourMills)));
		} catch (IOException e) {
			this.data = null;
			
			this.date.setTimeInMillis(this.nextHourMills);
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
		
		calendar.setTimeInMillis(startMills);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MINUTE, 0);
		this.nextHourMills = calendar.getTimeInMillis();
		
		nextDate();
		
		return getJSON(new JSONObject());
	}
	
}
