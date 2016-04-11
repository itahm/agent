package com.itahm.table;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.snmp.NodeList;

public class Device extends Table {

	private final static String STRING_SHUTDOWN = "shutdown";
	private final static String STRING_SNMP = "snmp";
	private final static String STRING_IFENTRY = "ifEntry";
	private final static String STRING_IP = "ip";
	
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
		
		// 장비를 모두 삭제한 경우 null
		if (idArray != null) {
			JSONObject device;
			String id;
			String peerID;
			
			// 신규로 추가된 device에게 ID 부여하는 역할과
			// 링크 확인의 역할을 수행함 
			for (int i=0, _i=idArray.length; i<_i; i++) {
				id = idArray[i];
				
				device = data.getJSONObject(id);
				
				// ID 부여
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
				
				if (device.has(STRING_IFENTRY)) {
					JSONObject ifEntry = device.getJSONObject(STRING_IFENTRY);
					String [] ifArray = JSONObject.getNames(ifEntry);
					
					if (ifArray != null) {
						for (int j=0, _j=ifArray.length; j<_j; j++) {
							peerID = ifArray[j];
							
							if ("".equals(ifEntry.getString(peerID))) {
								try {
									ifEntry.put(peerID, NodeList.findInterface(device.getString(STRING_IP), data.getJSONObject(peerID).getString(STRING_IP)));
								}
								catch (JSONException jsone) {
									// 그럴리는 없지만 혹시
								}
							}
						}
					}
				}
				// 없으면 안되는 것이지만 개발자 실수에 의해 없는 채로 넘어와서 예외 발생시키는 상황 방지
				else {
					device.put(STRING_IFENTRY, new JSONObject());
				}
			}
		}
		
		super.save(data);
	}
}
