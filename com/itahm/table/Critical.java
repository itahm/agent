package com.itahm.table;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.ITAhM;

public class Critical extends Table {
	
	public Critical() throws IOException {
		load("critical");
	}
	
	public JSONObject put(String ip, JSONObject critical) {
		ITAhM.agent.snmp.resetCritical(ip, critical);
		
		return super.put(ip, critical);
	}
}
