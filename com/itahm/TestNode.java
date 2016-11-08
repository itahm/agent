package com.itahm;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.snmp.TmpNode;
import com.itahm.table.Table;

public class TestNode extends TmpNode {

	public TestNode(SNMPAgent agent, String ip) {
		super(agent, ip);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onSuccess(String profileName) {
		JSONObject device = ITAhM.getTable(Table.DEVICE).getJSONObject(super.ip);
		Table monitorTable = ITAhM.getTable(Table.MONITOR);
		
		if (!device.has("name") || "".equals(device.getString("name"))) {
			device.put("name", super.sysName);
		}
		
		ITAhM.agent.icmp.removeNode(ip);
		
		monitorTable.getJSONObject().put(super.ip, new JSONObject()
			.put("protocol", "snmp")
			.put("ip", super.ip)
			.put("profile", profileName)
			.put("shutdown", false)
			.put("critical", false));
		
		monitorTable.save();
		
		super.agent.addNode(this.ip, profileName);
		
		try {
			ITAhM.log.write(ip, String.format("%s [%s] SNMP 등록 성공.", super.ip, super.sysName), "", true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onFailure() {
		try {
			ITAhM.log.write(ip, String.format("%s SNMP 등록 실패.", super.ip), "shutdown", false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
