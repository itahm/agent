package com.itahm.request;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

 // TODO: Auto-generated Javadoc
/**
  * The Class Request.
  */
 abstract public class Request {
	
	 protected void request(JSONObject request) {
		JSONObject value = null;
		
		try {
			String command = request.getString("command");
			JSONObject data;
			
			if (request.isNull("data")) {
				value = execute(command);
				
				request.put("data", value == null? JSONObject.NULL: value);
			}
			else {
				data = request.getJSONObject("data");
				
				@SuppressWarnings("rawtypes")
				Iterator it = data.keys();
				String key;
				
				while (it.hasNext()) {
					key = (String)it.next();
					value = execute(command, key, data.isNull(key)? null: data.getJSONObject(key));
					
					data.put(key, value == null? JSONObject.NULL: value);
				}
			}
		}
		catch (JSONException jsone) {
			jsone.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected JSONObject execute(JSONObject data, String command, String key, JSONObject value) {
		JSONObject result = null;
		
		if ("get".equals(command)) {
			if (data.has(key)) {
				result = data.getJSONObject(key);
			}
		}
		else if ("put".equals(command)) {
			data.put(key, value);
			
			result = value;
		}
		else if ("delete".equals(command)) {
			result = (JSONObject)data.remove(key);
		}
		
		return result;
	}
	
	abstract protected JSONObject execute(String command, String key, JSONObject value);
	abstract protected JSONObject execute(String command);
}
