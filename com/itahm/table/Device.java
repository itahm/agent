package com.itahm.table;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.Constant;
import com.itahm.ITAhM;

public class Device extends Table {

	public final static String SHUTDOWN = "shutdown";
	public final static String IFENTRY = "ifEntry";
	public final static String IP = "ip";
	public final static String NAME = "name";
	public final static String SNMP = "snmp";
	public final static String PROFILE = "profile";
	public final static String TYPE = "type";
	public final static String X = "x";
	public final static String Y = "y";
	public final static String LOCALHOST = "127.0.0.1";
	
	public Device() throws IOException {
		load("device");
		
		if (isEmpty()) {
			getJSONObject().put(LOCALHOST, new JSONObject()
				.put(IP, LOCALHOST)
				.put(X, 0)
				.put(Y, 0)
				.put(NAME, "localhost")
				.put(TYPE, "server")
				.put(IFENTRY, new JSONObject())
				.put(SHUTDOWN, false)
				.put("status", true)
			);
			
			save();
		}
		else {
			String [] ipArray = JSONObject.getNames(this.table);
			JSONObject device;
			
			// 모든 device는 active 하다고 가정하고 시작. shutdown 상태의 변화는 저장하지 않기 때문 
			for (int i=0, length=ipArray.length; i<length; i++) {
				device = this.table.getJSONObject(ipArray[i]);
				
				device.put(SHUTDOWN, false);
				device.put("status", true);
			}
		}
	}
	
	public void save(JSONObject data) {
		JSONObject device;
		String peerIP;
		String ip;
		
		// 링크 확인의 역할
		// SNMPAgent reload
		for (Object key : data.keySet()) {
			ip = (String)key;
			
			device = data.getJSONObject(ip);
			
			if (device.has(IFENTRY)) {
				JSONObject ifEntry = device.getJSONObject(IFENTRY);
				String [] ifArray = JSONObject.getNames(ifEntry);
				JSONObject peerDevice;
				
				if (ifArray != null) {
					for (int j=0, _j=ifArray.length; j<_j; j++) {
						peerIP = ifArray[j];
						peerDevice = data.getJSONObject(peerIP);
						
						if ("".equals(ifEntry.getString(peerIP))) {
							try {
								ifEntry.put(peerIP, ITAhM.snmp.getPeerIFName(device.getString(Constant.STRING_IP), peerDevice.getString(Constant.STRING_IP)));
							}
							catch (JSONException jsone) {
								// 그럴리는 없지만 혹시
								jsone.printStackTrace();
							}
						}
					}
				}
			}
			// 없으면 안되는 것이지만 개발자 실수에 의해 없는 채로 넘어와서 예외 발생시키는 상황 방지
			else {
				device.put(IFENTRY, new JSONObject());
			}
		}
		
		super.save(data);
		
		ITAhM.snmp.reStart();
	}
}
