package com.itahm.table;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.ITAhM;

public class Snmp extends Table {
	
	public Snmp() throws IOException {
		load(SNMP);
	}

	public JSONObject remove(String ip) {
		JSONObject snmp = super.remove(ip);
		
		if (snmp != null) {
			ITAhM.agent.removeNode(ip);
			
			ITAhM.getTable(Table.CRITICAL).remove(ip);
		}
		
		return snmp;
	}
	
	public JSONObject put(String ip, JSONObject node) {
		if (node == null) {
			remove(ip);
		}
		else {
			ITAhM.agent.testNode(ip);
		}
		
		return super.table;
	}
}
