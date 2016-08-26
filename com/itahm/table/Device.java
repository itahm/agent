package com.itahm.table;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.ITAhM;

public class Device extends Table {
	
	public Device() throws IOException {
		load("device");
		
		if (isEmpty()) {
			getJSONObject().put("127.0.0.1", new JSONObject()
				.put("ip", "127.0.0.1")
				.put("type", "server")
				.put("ifEntry", new JSONObject())
			);
			
			save();
		}
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
			
			ITAhM.getTable(Table.POSITION).remove(ip);
			ITAhM.getTable(Table.SNMP).remove(ip);
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
		}
		else {
			Table posTable = ITAhM.getTable(Table.POSITION);
			
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
		}
		
		return super.put(ip, device);
	}
	
}
