package com.itahm;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.itahm.json.JSONException;
import com.itahm.json.JSONObject;

import com.itahm.icmp.ICMPListener;
import com.itahm.json.RollingFile;
import com.itahm.snmp.Node;
import com.itahm.table.Table;

public class SNMPNode extends Node implements ICMPListener, Closeable {
	
	public enum Rolling {
		HRPROCESSORLOAD("hrProcessorLoad"),
		IFINBYTES("ifInBytes"),
		IFOUTBYTES("ifOutBytes"),
		IFINOCTETS("ifInOctets"),
		IFOUTOCTETS("ifOutOctets"),
		IFINERRORS("ifInErrors"),
		IFOUTERRORS("ifOutErrors"),
		HRSTORAGEUSED("hrStorageUsed"),
		RESPONSETIME("responseTime");
		
		private String database;
		
		private Rolling(String database) {
			this.database = database;
		}
		
		public String toString() {
			return this.database;
		}
	}
	
	private final File nodeRoot;
	private final Map<Rolling, HashMap<String, RollingFile>> rollingMap = new HashMap<Rolling, HashMap<String, RollingFile>>();
	private final String ip;
	private final SNMPAgent agent;
	private final Table deviceTable;
	private final ICMPNode icmp;
	private long responseTime;
	private long lastRolling = 0;
	private Critical critical;
	
	public SNMPNode(SNMPAgent agent, String ip, int udp, String community, JSONObject criticalCondition) throws IOException {
		super(agent, ip, udp, community, Agent.MAX_TIMEOUT);
		
		this.agent = agent;
		this.ip = ip;
		deviceTable = Agent.getTable(Table.DEVICE);
		nodeRoot = new File(agent.nodeRoot, ip);
		nodeRoot.mkdirs();
		
		for (Rolling database : Rolling.values()) {
			rollingMap.put(database, new HashMap<String, RollingFile>());
			
			new File(nodeRoot, database.toString()).mkdir();
		}
		
		icmp = new ICMPNode(this, ip);
		
		setCritical(criticalCondition);
		
		icmp.start();
	}
	
	public String getAddress() {
		return this.ip;
	}
	
	private void putData(Rolling database, String index, long value) {
		Map<String, RollingFile> map = this.rollingMap.get(database);
		RollingFile rollingFile = map.get(index);
		
		if (rollingFile == null) {
			try {
				map.put(index, rollingFile = new RollingFile(new File(this.nodeRoot, database.toString()), index));
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		
		try {
			rollingFile._roll(value);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public JSONObject getData(String database, String index, long start, long end, boolean summary) {
		JSONObject data = null;
	
		try {
			RollingFile rollingFile = this.rollingMap.get(Rolling.valueOf(database.toUpperCase())).get(index);
			
			if (rollingFile != null) {
				data = rollingFile.getData(start, end, summary);
			}
		}
		catch (IllegalArgumentException iae) {
			iae.printStackTrace();
		}
		
		return data;
	}
	
	public void setCritical(JSONObject criticalCondition) {
		if (criticalCondition == null) {
			this.critical = null;
		}
		else {
			this.critical = new Critical(criticalCondition) {
				
				@Override
				public void onCritical(boolean isCritical, Resource resource, String index, long rate) {
					agent.onCritical(ip, isCritical, String.format("%s.%s %d%% %s.", resource, index, rate, isCritical? "임계 초과": "정상"));
				}};
		}
	}
	
	public String getIFNameFromARP(String mac) {
		Integer index = super.macTable.get(mac);
		
		if (index == null) {
			return null;
		}

		try {
			return super.data.getJSONObject("ifEntry").getJSONObject(index.toString()).getString("ifName");
		}
		catch(JSONException jsone) {
			jsone.printStackTrace();
		}
		
		return null;
	}
	
	public String getPeerIFName(SNMPNode peer){
		if (!super.data.has("ifEntry")) {
			return "";
		}
		
		JSONObject ifEntry = super.data.getJSONObject("ifEntry");
		JSONObject ifData;
		String mac;
		String name;
		
		for (Object index : ifEntry.keySet()) {
			ifData = ifEntry.getJSONObject((String)index);
			
			if (!ifData.has("ifPhysAddress")) {
				continue;
			}
			
			mac = ifData.getString("ifPhysAddress");
			
			if (!"".equals(mac)) {
				name = peer.getIFNameFromARP(mac);
				
				if (name != null) {
					return name;
				}
			}
		}
		
		return "";
	}
	
	private void processSuccess() {
		JSONObject data;
		JSONObject oldData;
		long max;
		long maxRate;
		long maxErr;
		long value;
		long capacity;
		long tmpValue;
		
		for (String network : super.networkTable.keySet()) {
			if ("127.0.0.1".equals(network)) {
				continue;
			}
			
			this.agent.onNetwork(network, super.networkTable.get(network));
		}
		
		for (String mac : super.arpTable.keySet()) {
			this.agent.onARP(mac, super.arpTable.get(mac), super.maskTable.get(super.macTable.get(mac)));
		}
	
		putData(Rolling.RESPONSETIME, "0", this.responseTime);
		
		this.agent.onSubmitTop(this.ip, SNMPAgent.Resource.RESPONSETIME, this.responseTime);
		
		max = 0;
		for(String index: super.hrProcessorEntry.keySet()) {
			value = super.hrProcessorEntry.get(index);
			
			putData(Rolling.HRPROCESSORLOAD, index, value);
			
			if (this.critical != null) {
				this.critical.analyze(Critical.Resource.PROCESSOR, index, 100, value);
			}
			
			max = Math.max(max, value);
		}
		
		this.agent.onSubmitTop(this.ip, SNMPAgent.Resource.PROCESSOR, max);
		
		max = 0;
		maxRate = 0;
		for(String index: super.hrStorageEntry.keySet()) {
			data = super.hrStorageEntry.get(index);
			
			capacity = data.getInt("hrStorageSize");
			tmpValue = data.getInt("hrStorageUsed");
			
			if (capacity <= 0) {
				continue;
			}
			
			value = 1L* tmpValue * data.getInt("hrStorageAllocationUnits");
			
			putData(Rolling.HRSTORAGEUSED, index, value);
			
			switch(data.getInt("hrStorageType")) {
			case 2:
				if (this.critical != null) {
					this.critical.analyze(Critical.Resource.MEMORY, index, capacity, tmpValue);
				}
				
				this.agent.onSubmitTop(this.ip, SNMPAgent.Resource.MEMORY, value);
				this.agent.onSubmitTop(this.ip, SNMPAgent.Resource.MEMORYRATE, tmpValue *100L / capacity);
				
				break;
			case 4:
				if (this.critical != null) {
					this.critical.analyze(Critical.Resource.STORAGE, index, capacity, tmpValue);
				}
				
				max = Math.max(max, value);
				maxRate = Math.max(maxRate, tmpValue *100L / capacity);
			}
		}
		
		this.agent.onSubmitTop(this.ip, SNMPAgent.Resource.STORAGE, max);
		this.agent.onSubmitTop(this.ip, SNMPAgent.Resource.STORAGERATE, maxRate);
		
		if (this.lastRolling > 0) {
			// 보관된 값
			JSONObject ifEntry = super.data.getJSONObject("ifEntry");
			JSONObject device = deviceTable.getJSONObject(this.ip);
			JSONObject ifSpeed = device.has("ifSpeed")? device.getJSONObject("ifSpeed"): null;
			long bytes;
			
			max = 0;
			maxRate = 0;
			maxErr = 0;
			
			for(String index: super.ifEntry.keySet()) {
				// 특정 index가 새로 생성되었다면 보관된 값이 없을수도 있음.
				if (!ifEntry.has(index)) {
					continue;
				}
				
				data = super.ifEntry.get(index);
				capacity = 0;
				
				oldData = ifEntry.getJSONObject(index);
				
				if (!data.has("ifAdminStatus") || data.getInt("ifAdminStatus") != 1) {
					continue;
				}
				
				if (ifSpeed !=null && ifSpeed.has(index)) {
					capacity = ifSpeed.getLong(index);
				}
				else if (data.has("ifHighSpeed")) {
					capacity = data.getLong("ifHighSpeed");
				}
				else if (capacity == 0 && data.has("ifSpeed")) {
					capacity = data.getLong("ifSpeed");
				}
				
				if (capacity <= 0) {
					continue;
				}
				
				if (data.has("ifInErrors")) {
					value = data.getInt("ifInErrors");
					
					if (oldData.has("ifInErrors")) {
						value -= oldData.getInt("ifInErrors");
						
						data.put("ifInErrors", value);
					
						putData(Rolling.IFINERRORS, index, value);
						
						maxErr = Math.max(maxErr, value);
					}
				}
				
				if (data.has("ifOutErrors")) {
					value = data.getInt("ifOutErrors");
					
					if (oldData.has("ifOutErrors")) {
						value -= oldData.getInt("ifOutErrors");
						
						data.put("ifOutErrors", value);
						
						putData(Rolling.IFOUTERRORS, index, value);
						
						maxErr = Math.max(maxErr, value);
					}
				}
				
				bytes = -1;
				
				if (data.has("ifHCInOctets") && oldData.has("ifHCInOctets")) {
					bytes = data.getLong("ifHCInOctets") - oldData.getLong("ifHCInOctets");
				}
				
				if (data.has("ifInOctets") && oldData.has("ifInOctets")) {
					bytes = Math.max(bytes, data.getLong("ifInOctets") - oldData.getLong("ifInOctets"));
				}
				
				if (bytes  >= 0) {
					bytes = bytes *8000 / (super.lastResponse - this.lastRolling);
					
					data.put("ifInBPS", bytes);
					
					putData(Rolling.IFINOCTETS, index, bytes);
					
					max = Math.max(max, bytes);
					maxRate = Math.max(maxRate, bytes *100L / capacity);
				}
				
				bytes = -1;
				
				if (data.has("ifHCOutOctets") && oldData.has("ifHCOutOctets")) {
					bytes = data.getLong("ifHCOutOctets") - oldData.getLong("ifHCOutOctets");
				}
				
				if (data.has("ifOutOctets") && oldData.has("ifOutOctets")) {
					bytes = Math.max(bytes, data.getLong("ifOutOctets") - oldData.getLong("ifOutOctets"));
				}
				else {
					continue;
				}
				
				if (bytes >= 0) {
					bytes = bytes *8000 / (super.lastResponse - this.lastRolling);
					
					data.put("ifOutBPS", bytes);
					
					putData(Rolling.IFOUTOCTETS, index, bytes);
					
					max = Math.max(max, bytes);
					maxRate = Math.max(maxRate, bytes *100L / capacity);
				}
				
				if (this.critical != null) {
					this.critical.analyze(Critical.Resource.THROUGHPUT, index, capacity, max);
				}
			}
			
			this.agent.onSubmitTop(this.ip, SNMPAgent.Resource.THROUGHPUT, max);
			this.agent.onSubmitTop(this.ip, SNMPAgent.Resource.THROUGHPUTRATE, maxRate);
			this.agent.onSubmitTop(this.ip, SNMPAgent.Resource.THROUGHPUTERR, maxErr);
		}
		
	}
	@Override
	public void onSuccess() {
		try {
			processSuccess();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		this.agent.onSubmitTop(this.ip, SNMPAgent.Resource.FAILURERATE, getFailureRate());
		
		this.lastRolling = super.lastResponse;
		
		this.agent.onResponse(this.ip);
	}

	@Override
	protected void onFailure() {
		this.agent.onSubmitTop(this.ip, SNMPAgent.Resource.FAILURERATE, getFailureRate());
		
		this.agent.onTimeout(this.ip);
	}
	
	@Override
	public void onSuccess(String host, long time) {
		this.responseTime = time;
		
		super.data.put("responseTime", time);
		
		this.agent.onSuccess(this.ip, time);
	}

	@Override
	public void onFailure(String host) {
		this.agent.onFailure(this.ip);
	}

	@Override
	public void close() throws IOException {
		this.icmp.stop();
	}
	
}