/*
 * 
 */
package com.itahm.request;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.json.RollingMap.Resource;
import com.itahm.snmp.Node;

public class Delay extends Request {

	public Delay(JSONObject request) {
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
		
		long start;
		long end;
		
		boolean summary = false;
		
		try {
			start = value.getLong("start");
			end = value.getLong("end");
		
			
			if (value.has("summary")) {
				summary = value.getBoolean("summary");
			}
		}
		catch (JSONException jsone) {
			return null;
		}
		
		Node node = Node.getNode(key);
		
		if (node == null) {
			return null;
		}
		
		return node.getData(Resource.RESPONSETIME, "0", start, end, summary);
	}
	
}