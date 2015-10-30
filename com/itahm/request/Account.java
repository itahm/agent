package com.itahm.request;

import org.json.JSONObject;

import com.itahm.Data;

public class Account extends Request {

	private final JSONObject data;
	private boolean isRoot = false;
	
	public Account(JSONObject request) {
		data = Data.getJSONObject(Data.Table.ACCOUNT);
		
		request(request);
	}
	
	public Account(JSONObject request, boolean root) {
		this(request);
		
		isRoot = root;
	}
	
	@Override
	protected JSONObject execute(String command) {
		if (!"get".equals(command)) {
			return null;
		}
		
		if (this.isRoot) {
			return this.data;
		}
		
		JSONObject result = new JSONObject();
		String [] names = JSONObject.getNames(this.data);
		String user;
		
		if (names != null) {
			for (int i=0, length=names.length; i<length; i++) {
				user = names[i];
				result.put(user, new JSONObject().put("username", user));
			}
		}
		
		return result;
	}
	
	@Override
	protected JSONObject execute(String command, String key, JSONObject value) {
		return execute(this.data, command, key, value);
	}
	
}
