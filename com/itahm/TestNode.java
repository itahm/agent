package com.itahm;

import java.io.IOException;

import com.itahm.json.JSONObject;

import com.itahm.snmp.TmpNode;
import com.itahm.table.Table;

public class TestNode extends TmpNode {

	private final SNMPAgent agent;
	private final boolean onFailure;
	
	public TestNode(SNMPAgent agent, String ip, boolean onFailure) {
		super(agent, ip, Agent.MAX_TIMEOUT);
		
		this.agent = agent;
		
		this.onFailure = onFailure;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onSuccess(String profileName) {
		Table deviceTable = Agent.getTable(Table.DEVICE);
		Table monitorTable = Agent.getTable(Table.MONITOR);
	
		if (deviceTable.getJSONObject(super.ip) == null) {
			deviceTable.put(super.ip, new JSONObject());
		}
		
		Agent.manager.icmp.removeNode(super.ip);
		
		monitorTable.getJSONObject().put(super.ip, new JSONObject()
			.put("protocol", "snmp")
			.put("ip", super.ip)
			.put("profile", profileName)
			.put("shutdown", false)
			.put("critical", false));
		
		monitorTable.save();
		
		this.agent.addNode(this.ip, profileName);
		
		try {
			Agent.manager.log.write(ip, String.format("%s SNMP 등록 성공.", super.ip), "", true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onFailure() {
		if (!onFailure) {
			return;
		}
		
		try {
			Agent.manager.log.write(ip, String.format("%s SNMP 등록 실패.", super.ip), "shutdown", false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
