package com.itahm.table;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.ITAhM;

public class Monitor extends Table {
	
	public Monitor() throws IOException {
		load(MONITOR);
	}

	public JSONObject remove(String ip) {
		JSONObject monitor = super.remove(ip);
		Table table;
		
		if (monitor != null) {
			if (monitor.getString("protocol") == "snmp") {
				if (ITAhM.agent.snmp.removeNode(ip)) {
					table = ITAhM.getTable(Table.CRITICAL);
					
					table.remove(ip);
					table.save();
				}
			}
			else {
				ITAhM.agent.icmp.removeNode(ip);
			}
			
			save();
		}
		
		return monitor;
	}
	
	public JSONObject put(String ip, JSONObject monitor) {
		remove(ip);
		
		if (monitor != null) {
			if ("snmp".equals(monitor.getString("protocol"))) {
				ITAhM.agent.snmp.testNode(ip);
			}
			else if ("icmp".equals(monitor.getString("protocol"))) {
				ITAhM.agent.icmp.testNode(ip);
			}
		}
		
		return super.table;
	}
}
