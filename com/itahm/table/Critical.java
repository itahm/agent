package com.itahm.table;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.ITAhM;

public class Critical extends Table {
	
	public Critical() throws IOException {
		load("critical");
	}
	
	public void save(JSONObject data) {
		if (data.has("target")) {
			String ip = (String)data.remove("target");
			
			ITAhM.snmp.reload(ip);
		}
		else {
			//TODO 오류
			System.out.println("Critical.java critical data 에서 target 없음");
		}
		
		super.save(data);
	}
}
