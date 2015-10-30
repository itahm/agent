package com.itahm.request;

import org.json.JSONObject;

import com.itahm.Data;
import com.itahm.SnmpManager;

public class Snmp extends Request {

	private final JSONObject data;
	
	public Snmp(JSONObject request) {
		data = Data.getJSONObject(Data.Table.SNMP);
		
		request(request);
	}

	@Override
	protected JSONObject execute(String command) {
		if (!"get".equals(command)) {
			return null;
		}
		
		return this.data;
	}
	
	@Override
	protected JSONObject execute(String command, String key, JSONObject value) {
		if (!this.data.has(key)) {
			return null;
		}
			
		if ("realtime".equals(command)) {
			SnmpManager.addRealTimeNode(key);
		}
		else if ("get".equals(command)) {
		}
		else {
			return null;
		}
			
		return data.getJSONObject(key);
	}
	
}
