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
	private final long end;
	private long nextHourMills;
	private long day;
	private JSONObject data = null;
	private final JSONObject result;
	
	public JSONData(JSONObject json, File rollingRoot, long startDate, long endDate) {
		Calendar calendar = Calendar.getInstance();
		
		date = Calendar.getInstance();
		
		result = json;
		root = rollingRoot;
		date.setTimeInMillis(startDate);
		end = endDate;
		
		date.set(Calendar.MILLISECOND, 0);
		date.set(Calendar.SECOND, 0);	
		
		calendar.setTimeInMillis(startDate);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MINUTE, 0);
		nextHourMills = calendar.getTimeInMillis();
		
		day = calendar.get(Calendar.DATE);
		
		nextDate();
	}
	
	public boolean next() {
		if (this.end < this.date.getTimeInMillis()) {
			return false;
		}
		
		long date = this.date.getTimeInMillis();
		String key = Long.toString(date);
		
		if (date >= this.nextHourMills) {
			nextDate();
		}
		
		try {
			if (this.data != null && this.data.has(key)) {
				this.result.put(key, this.data.getLong(key));
			}
		}
		catch (JSONException jsone) {
		}
		
		this.date.add(Calendar.MINUTE, 1);
		
		return true;
	}
	
	/** next hour or next day*/
	private void nextDate() {
		Calendar calendar = Calendar.getInstance();
		long hourMills = this.nextHourMills;
		int day;
		
		calendar.setTimeInMillis(this.nextHourMills);
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		
		this.nextHourMills = calendar.getTimeInMillis();
		
		day = calendar.get(Calendar.DATE);
		if (day != this.day) {
			// 날짜가 바뀌었으면 calendar.set(Calendar.HOUR_OF_DAY, 0) 는 불필요. 단지 최종 날짜만 바꾸어준다.
			this.day = day;
		}
		else {
			// 바뀌지 않았으면
			calendar.set(Calendar.HOUR_OF_DAY, 0);
		}
		
		try {
			File dir = new File(this.root, Long.toString(calendar.getTimeInMillis()));
			File file = new File(dir, Long.toString(hourMills));
			
			this.data = JSONFile.getJSONObject(file);
		} catch (IOException e) {
			this.data = null;
			
			this.date.setTimeInMillis(this.nextHourMills);
		}
	}
	
}
