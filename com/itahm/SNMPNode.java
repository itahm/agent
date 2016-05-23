package com.itahm;

import java.io.File;
import java.io.IOException;

import org.json.JSONObject;

import com.itahm.json.RollingFile;
import com.itahm.json.RollingMap;
import com.itahm.json.RollingMap.Resource;
import com.itahm.snmp.Node;

public class SNMPNode extends Node {

	private final SNMPAgent agent;
	private final RollingMap rollingMap;
	private final String ip;
	
	private long lastRolling;
	
	public SNMPNode(SNMPAgent agent, String ip, int udp, String community) throws IOException {
		super(agent.getSNMP(), ip, udp, community);
		
		this.agent = agent;
		this.ip = ip;
		
		File nodeRoot = new File(new File(ITAhM.getRoot(), Constant.STRING_SNMP), ip);
		nodeRoot.mkdirs();
		
		rollingMap = new RollingMap(nodeRoot);
	}
	
	public JSONObject getData(String database, String index, long start, long end, boolean summary) {
		JSONObject data = null;
	
		try {
			RollingFile rollingFile = this.rollingMap.getFile(Resource.valueOf(database.toUpperCase()), index);
			
			if (rollingFile != null) {
				data = rollingFile.getData(start, end, summary);
			}
		}
		catch (IllegalArgumentException iae) {
			iae.printStackTrace();
		}
		
		return data;
	}
	
	public String getIFNameFromARP(String mac) {
		JSONObject arpTable = this.data.getJSONObject("arpTable");
		
		if (arpTable.has(mac)) {
			JSONObject ifEntry = this.data.getJSONObject("ifEntry");
			String index = arpTable.getString(mac);
			
			return ifEntry.getJSONObject(index).getString(Constant.STRING_IFNAME);
		}
		
		return null;
	}
	
	public String getPeerIFName(SNMPNode peer){
		JSONObject ifEntry = this.data.getJSONObject(Constant.STRING_IFINDEX);
		String mac;
		String name;
		
		for (Object index : ifEntry.keySet()) {
			mac = ifEntry.getJSONObject((String)index).getString(Constant.STRING_MAC_ADDR);
			
			if (!"".equals(mac)) {
				name = peer.getIFNameFromARP(mac);
				
				if (name != null) {
					return name;
				}
			}
		}
		
		return "";
	}
	
	@Override
	public void onSuccess() {
		JSONObject data;
		JSONObject oldData;
		
		this.agent.onSuccess(this.ip);
		
		this.rollingMap.put(Resource.RESPONSETIME, "0", this.responseTime);
		
		for(String index: this.hrProcessorEntry.keySet()) {
			this.rollingMap.put(Resource.HRPROCESSORLOAD, index, this.hrProcessorEntry.get(index));
		}
		
		for(String index: this.hrStorageEntry.keySet()) {
			data = this.hrStorageEntry.get(index);
			
			this.rollingMap.put(Resource.HRSTORAGEUSED, index, 1L* data.getInt("hrStorageUsed") * data.getInt("hrStorageAllocationUnits"));
		}
		
		if (lastRolling > 0) {
			long in;
			long out;
			long interval = this.data.getLong("lastResponse") - lastRolling;
			
			for(String index: this.ifEntry.keySet()) {
				data = this.ifEntry.get(index);
				oldData = this.data.getJSONObject("ifEntry").getJSONObject(index);
				
				if (data.has(Constant.STRING_IFHCOUT)) {
					out = data.getLong(Constant.STRING_IFHCOUT);
				}
				else {
					out = data.getInt(Constant.STRING_IFOUT);
				}
				
				if (oldData.has(Constant.STRING_IFHCOUT)) {
					out -= oldData.getLong(Constant.STRING_IFHCOUT);
				}
				else {
					out -= data.getInt(Constant.STRING_IFOUT);
				}
				
				this.rollingMap.put(Resource.IFOUTOCTETS, index, out *8 / interval);
				
				if (data.has(Constant.STRING_IFHCIN)) {
					in = data.getLong(Constant.STRING_IFHCIN);
				}
				else {
					in = data.getInt(Constant.STRING_IFIN);
				}
				
				if (oldData.has(Constant.STRING_IFHCIN)) {
					in -= oldData.getLong(Constant.STRING_IFHCIN);
				}
				else {
					in -= data.getInt(Constant.STRING_IFIN);
				}
				
				this.rollingMap.put(Resource.IFINOCTETS, index, in *8 / interval);
			}
		}
		
		lastRolling = this.data.getLong("lastResponse");
		
	}

	@Override
	protected void onFailure() {
		this.agent.onFailure(this.ip);
	}

}
