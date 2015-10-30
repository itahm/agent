package com.itahm.request;

import org.json.JSONObject;

import com.itahm.Data;
import com.itahm.SnmpManager;

public class RealTime extends Request {

	public RealTime(JSONObject request) {
		request(request);
	}
	
	@Override
	protected JSONObject execute(String command) {
		return null;
	}
	
	@Override
	protected JSONObject execute(String command, String key, JSONObject value) {
		if (!"get".equals(command)) {
			return null;
		}
		
		JSONObject data = Data.getJSONObject(Data.Table.SNMP);
		
		if (data.has(key)) {
			SnmpManager.addRealTimeNode(key);
			
			JSONObject device = data.getJSONObject(key);
			
			return device;
		}
		
		return null;
	}
	
}