package com.itahm;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.snmp.TmpNode;
import com.itahm.table.Table;

public class TestNode extends TmpNode {

	private final boolean onFailure;
	public TestNode(SNMPAgent agent, String ip, boolean onFailure) {
		super(agent, ip);
		
		this.onFailure = onFailure;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onSuccess(String profileName) {
		Table deviceTable = ITAhM.getTable(Table.DEVICE);
		Table monitorTable = ITAhM.getTable(Table.MONITOR);
		JSONObject device = deviceTable.getJSONObject(super.ip);
		
		String name = "";
	
		if (device == null) {
			deviceTable.put(super.ip, new JSONObject()
				.put("name", name = super.sysName)
			);
		}
		else {
			name = device.getString("name");
		
			if ("".equals(name)) {
				device.put("name", name = super.sysName);
				
				deviceTable.save();
			}
		}
		
		ITAhM.agent.icmp.removeNode(super.ip);
		
		monitorTable.getJSONObject().put(super.ip, new JSONObject()
			.put("protocol", "snmp")
			.put("ip", super.ip)
			.put("profile", profileName)
			.put("shutdown", false)
			.put("critical", false));
		
		monitorTable.save();
		
		super.agent.addNode(this.ip, profileName);
		
		try {
			ITAhM.log.write(ip, String.format("%s [%s] SNMP 등록 성공.", super.ip, name), "", true);
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
