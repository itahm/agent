package com.itahm.table;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.ITAhM;

public class Device extends Table {

	private final static String STRING_SHUTDOWN = "shutdown";
	private final static String STRING_SNMP = "snmp";
	
	public Device() throws IOException {
		load("device");
		
		if (isEmpty()) {
			getJSONObject().put("0", new JSONObject()
				.put("id", "0")
				.put("ip", "127.0.0.1")
				.put("x", 0).put("y", 0)
				.put("name", "localhost")
				.put("snmp", true)
				.put("profile", "public")
				.put("type", "server")
				.put("shutdown", false)
				.put("ifEntry", new JSONObject())
			);
			
			save();
		}
		else {
			String [] idArray = JSONObject.getNames(this.table);
			JSONObject device;
			
			// 모든 device는 active 하다고 가정하고 시작. shutdown 상태의 변화는 저장하지 않기 때문 
			for (int i=0, length=idArray.length; i<length; i++) {
				device = this.table.getJSONObject(idArray[i]);
				
				device.put(STRING_SHUTDOWN, false);
			}
		}
	}
	
	public void save(JSONObject data) {
		String [] idArray = JSONObject.getNames(data);
		JSONObject device;
		String id;
		
		for (int i=0, length=idArray.length; i<length; i++) {
			id = idArray[i];
			
			device = data.getJSONObject(id);
			
			if (Long.parseLong(id) < 0) {
				data.remove(id);
				
				try {
					id = Long.toString(((Index)ITAhM.getTable("index")).assign());
					
					// snmp 상태가 제거되어야 함을 확인
					device.remove(STRING_SNMP);
					
					device.put("id", id);
					device.put("shutdown", false);
					
					data.put(id, device);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		super.save(data);
	}
}
