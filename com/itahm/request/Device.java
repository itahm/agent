package com.itahm.request;

import org.json.JSONObject;

import com.itahm.Data;

public class Device extends Request {
	
	private final JSONObject data;
	
	public Device(JSONObject request) {
		data = Data.getJSONObject(Data.Table.DEVICE);
		
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
		if ("put".equals(command) && Integer.parseInt(key) < 0) {
			int id = Data.newID();
				
			if (id < 0) {
				return null;
			}
				
			value.put("id", key = Integer.toString(id));
		}
		
		return execute(this.data, command, key, value);
	}
	
}
