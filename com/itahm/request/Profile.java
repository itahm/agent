package com.itahm.request;

import org.json.JSONObject;

import com.itahm.Data;

public class Profile extends Request {

	private final JSONObject data;
	
	public Profile(JSONObject request) {
		data = Data.getJSONObject(Data.Table.PROFILE);
		
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
		return execute(data, command, key, value);
	}
	
}
