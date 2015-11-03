/*
 * 
 */
package com.itahm.request;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.json.RollingMap.Resource;
import com.itahm.snmp.Node;

public class Processor extends Request {

	public Processor(JSONObject request) {
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
		int index;
		boolean summary = false;
		
		try {
			start = value.getLong("start");
			end = value.getLong("end");
			index = value.getInt("index");
			
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
		
		return node.getData(Resource.HRPROCESSORLOAD, Integer.toString(index), start, end, summary);
	}
	
}