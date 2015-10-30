/*
 * 
 */
package com.itahm.request;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.json.RollingMap.Resource;
import com.itahm.snmp.Node;

public class OutOctet extends Request {

	public OutOctet(JSONObject request) {
		request(request);
	}

	@Override
	protected JSONObject execute(String command) {
		return null;
	}
	
	@Override
	protected JSONObject execute(String command, String key, JSONObject data) {
		if (!"get".equals(command)) {
			return null;
		}
		
		long start;
		long end;
		int index;
		boolean summary = false;
		
		try {
			start = data.getLong("start");
			end = data.getLong("end");
			index = data.getInt("index");
			
			if (data.has("summary")) {
				summary = data.getBoolean("summary");
			}
		}
		catch (JSONException jsone) {	
			return null;
		}
		
		Node node = Node.getNode(key);
		
		if (node == null) {
			return null;
		}
		
		return node.getData(Resource.IFOUTOCTETS, Integer.toString(index), start, end, summary);
	}
	
}