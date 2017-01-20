package com.itahm.table;

import java.io.File;
import java.io.IOException;

import com.itahm.json.JSONObject;

import com.itahm.Agent;

public class Device extends Table {
	
	public Device(File dataRoot) throws IOException {
		super(dataRoot, DEVICE);
	}
	
	public JSONObject remove (String ip) {
		JSONObject device = super.remove(ip);
		
		if (device != null) {
			JSONObject linkData = device.getJSONObject("ifEntry");
			
			if (linkData.length() > 0) {
				for (Object peerIP : linkData.keySet()) {
					super.table.getJSONObject((String)peerIP).getJSONObject("ifEntry").remove(ip);
				}
				
				save();
			}
			
			Agent.getTable(Table.POSITION).remove(ip);
			Agent.getTable(Table.MONITOR).remove(ip);
		}
		
		return device;
	}
	/**
	 * 추가인 경우 position 정보를 생성해 주어야 하고
	 * 삭제인 경우 position 정보와 snmp 정보를 함께 삭제해 주어야 한다.
	 */
	public JSONObject put(String ip, JSONObject device) {
		if (device == null) {
			remove(ip);
			
			return super.table;
		}
		
		Table posTable = Agent.getTable(Table.POSITION);
		
		if (posTable.getJSONObject(ip) == null) {
			posTable.put(ip, new JSONObject().put("x", 0).put("y", 0));
		}
		
		if (!device.has("name")) {
			device.put("name", "");
		}
		
		if (!device.has("ip")) {
			device.put("ip", ip);
		}
		
		if (!device.has("type")) {
			device.put("type", "unknown");
		}
		
		if (!device.has("ifEntry")) {
			device.put("ifEntry", new JSONObject());
		}
		
		if (!device.has("ifSpeed")) {
			device.put("ifSpeed", new JSONObject());
		}
		
		return super.put(ip, device);
	}
	
}
