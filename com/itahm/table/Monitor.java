package com.itahm.table;

import java.io.File;
import java.io.IOException;

import com.itahm.json.JSONObject;

import com.itahm.Agent;

public class Monitor extends Table {
	
	public Monitor(File dataRoot) throws IOException {
		super(dataRoot, MONITOR);
	}

	public JSONObject remove(String ip) {
		JSONObject monitor = super.remove(ip);
		Table table;
		
		if (monitor != null) {
			if ("snmp".equals(monitor.getString("protocol"))) {
				if (Agent.manager.snmp.removeNode(ip)) {
					table = Agent.getTable(Table.CRITICAL);
					
					table.remove(ip);
				}
				else {
					// 오류
					new RuntimeException().printStackTrace();
				}
			}
			else if ("icmp".equals(monitor.getString("protocol"))) {
				Agent.manager.icmp.removeNode(ip);
			}
			else {
				// 오류
				new RuntimeException().printStackTrace();
			}
			
			save();
		}
		else {
			// 정상
		}
		
		return monitor;
	}
	
	public JSONObject put(String ip, JSONObject monitor) {
		remove(ip);
		
		if (monitor != null) {
			if ("snmp".equals(monitor.getString("protocol"))) {
				Agent.manager.snmp.testNode(ip);
			}
			else if ("icmp".equals(monitor.getString("protocol"))) {
				Agent.manager.icmp.testNode(ip);
			}
		}
		
		return super.table;
	}
}
