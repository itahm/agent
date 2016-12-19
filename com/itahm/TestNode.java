package com.itahm;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.snmp.TmpNode;
import com.itahm.table.Table;

public class TestNode extends TmpNode {

	private final SNMPAgent agent;
	private final boolean onFailure;
	
	public TestNode(SNMPAgent agent, String ip, boolean onFailure) {
		super(agent, ip);
		
		this.agent = agent;
		
		this.onFailure = onFailure;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onSuccess(String profileName) {
		Table deviceTable = ITAhM.getTable(Table.DEVICE);
		Table monitorTable = ITAhM.getTable(Table.MONITOR);
	
		if (deviceTable.getJSONObject(super.ip) == null) {
			deviceTable.put(super.ip, new JSONObject());
		}
		
		ITAhM.agent.icmp.removeNode(super.ip);
		
		monitorTable.getJSONObject().put(super.ip, new JSONObject()
			.put("protocol", "snmp")
			.put("ip", super.ip)
			.put("profile", profileName)
			.put("shutdown", false)
			.put("critical", false));
		
		monitorTable.save();
		
		this.agent.addNode(this.ip, profileName);
		
		try {
			ITAhM.log.write(ip, String.format("%s SNMP 등록 성공.", super.ip), "", true);
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
			ITAhM.log.write(ip, String.format("%s SNMP 등록 실패.", super.ip), "shutdown", false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
