package com.itahm.snmp;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

public class Address {

	private final Map<String, String> map;
	private final JSONObject database;
	
	public Address() {
		this(new JSONObject());
	}

	public Address(JSONObject jo) {
		map = new HashMap<String, String>();
		database = jo;
	}
	
	public void put(String ip) {
		if (!this.map.containsKey(ip)) {
			this.map.put(ip, null);
		}
	}
	
	public void put(String ip, String mac) {
		if (!this.map.containsKey(ip)) {
			return;
		}
		
		this.map.put(ip, mac);
		
		long now = Calendar.getInstance().getTimeInMillis();
		JSONObject jo;
		
		if (this.database.has(ip)) {
			jo = this.database.getJSONObject(ip);
			
			if (mac.equals(jo.getString("mac"))) {
				jo.put("last", now);
				
				return;
			}
		}
		else {
			this.database.put(ip, jo = new JSONObject());
		}
		
		jo.put("mac", mac);
		jo.put("from", now);
		jo.put("last", now);
	}
	
	public JSONObject getJSONObject() {
		return this.database;
	}
}
