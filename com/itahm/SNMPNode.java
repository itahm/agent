package com.itahm;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.itahm.json.RollingFile;
import com.itahm.json.RollingMap;
import com.itahm.json.RollingMap.Resource;
import com.itahm.snmp.Node;

public class SNMPNode extends Node {

	private final RollingMap rollingMap;
	private final String ip;
	private final SNMPAgent agent;
	private final int timeout;
	private long lastRolling;
	private CriticalData critical;
	
	public SNMPNode(SNMPAgent agent, String ip, int udp, String community, int timeout, JSONObject criticalCondition) throws IOException {
		super(agent, ip, udp, community, timeout);
		
		this.agent = agent;
		this.ip = ip;
		this.timeout = timeout;
		
		File nodeRoot = new File(agent.nodeRoot, ip);
		nodeRoot.mkdirs();
		
		rollingMap = new RollingMap(nodeRoot);
		
		setCritical(criticalCondition);
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
	
	public void setCritical(JSONObject criticalCondition) {
		if (criticalCondition == null) {
			this.critical = null;
		}
		else {
			this.critical = new CriticalData(criticalCondition);
		}
	}
	
	public String getIFNameFromARP(String mac) {
		Integer index = super.macTable.get(mac);
		
		if (index != null) {
			JSONObject ifEntry = super.data.getJSONObject("ifEntry");
			
			return ifEntry.getJSONObject(index.toString()).getString("ifName");
		}
		
		return null;
	}
	
	public String getPeerIFName(SNMPNode peer){
		JSONObject ifEntry = super.data.getJSONObject("ifEntry");
		String mac;
		String name;
		
		for (Object index : ifEntry.keySet()) {
			mac = ifEntry.getJSONObject((String)index).getString("ifPhysAddress");
			
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
		long max;
		long maxRate;
		long maxErr;
		long value;
		long capacity;
		long tmpValue;
		
		this.agent.onSuccess(this.ip);
		
		for (String network : super.networkTable.keySet()) {
			if ("127.0.0.1".equals(network)) {
				continue;
			}
			
			this.agent.onNetwork(network, super.networkTable.get(network));
		}
		
		for (String mac : super.arpTable.keySet()) {
			this.agent.onARP(mac, super.arpTable.get(mac), super.maskTable.get(super.macTable.get(mac)));
		}
	
		this.rollingMap.put(Resource.RESPONSETIME, "0", super.responseTime);
		
		if (this.critical != null) {
			this.critical.analyze(CriticalData.RESPONSETIME, "0", this.timeout, super.responseTime);
		}
		
		this.agent.onSubmitTop(this.ip, "responseTime", super.responseTime);
		
		max = 0;
		for(String index: super.hrProcessorEntry.keySet()) {
			value = super.hrProcessorEntry.get(index);
			
			this.rollingMap.put(Resource.HRPROCESSORLOAD, index, value);
			if (this.critical != null) {
				this.critical.analyze(CriticalData.PROCESSOR, index, 100, value);
			}
			
			max = Math.max(max, value);
		}
		
		this.agent.onSubmitTop(this.ip, "processor", max);
		
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
			this.rollingMap.put(Resource.HRSTORAGEUSED, index, value);
			
			switch(data.getInt("hrStorageType")) {
			case 2:
				if (this.critical != null) {
					this.critical.analyze(CriticalData.MEMORY, index, capacity, tmpValue);
				}
				
				this.agent.onSubmitTop(this.ip, "memory", value);
				this.agent.onSubmitTop(this.ip, "memoryRate", tmpValue *100L / capacity);
				
				break;
			case 4:
				if (this.critical != null) {
					this.critical.analyze(CriticalData.STORAGE, index, capacity, tmpValue);
				}
				
				max = Math.max(max, value);
				maxRate = Math.max(maxRate, tmpValue *100L / capacity);
			}
		}
		
		this.agent.onSubmitTop(this.ip, "storage", max);
		this.agent.onSubmitTop(this.ip, "storageRate", maxRate);
		
		if (lastRolling > 0) {
			long interval = super.data.getLong("lastResponse") - lastRolling;
			// 보관된 값
			JSONObject ifEntry = super.data.getJSONObject("ifEntry");
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
				
				if (data.getInt("ifAdminStatus") != 1) {
					continue;
				}
				
				if (data.has("ifHighSpeed")) {
					capacity = data.getLong("ifHighSpeed");
				}
				
				if (capacity == 0 && data.has("ifSpeed")) {
					capacity = data.getLong("ifSpeed");
				}
				
				if (capacity == 0) {
					continue;
				}
				
				if (data.has("ifInErrors")) {
					value = data.getInt("ifInErrors");
					
					if (oldData.has("ifInErrors")) {
						value -= oldData.getInt("ifInErrors");
						
						data.put("ifInErrors", value);
					
						this.rollingMap.put(Resource.IFINERRORS, index, value);
						
						maxErr = Math.max(maxErr, value);
					}
				}
				
				if (data.has("ifOutErrors")) {
					value = data.getInt("ifOutErrors");
					
					if (oldData.has("ifOutErrors")) {
						value -= oldData.getInt("ifOutErrors");
						
						data.put("ifOutErrors", value);
						
						this.rollingMap.put(Resource.IFOUTERRORS, index, value);
						
						maxErr = Math.max(maxErr, value);
					}
				}
				
				bytes = -1;
				
				if (data.has("ifHCInOctets") && oldData.has("ifHCInOctets")) {
					bytes = data.getLong("ifHCInOctets") - oldData.getLong("ifHCInOctets");
				}
				else if (data.has("ifInOctets") && oldData.has("ifInOctets")) {
					bytes = data.getLong("ifInOctets") - oldData.getLong("ifInOctets");
				}
				
				if (bytes  >= 0) {
					bytes = bytes *8000 / interval;
					
					data.put("ifInBPS", bytes);
					
					this.rollingMap.put(Resource.IFINOCTETS, index, bytes);
					
					max = Math.max(max, bytes);
					maxRate = Math.max(maxRate, bytes *100L / capacity);
				}
				
				bytes = -1;
				
				if (data.has("ifHCOutOctets") && oldData.has("ifHCOutOctets")) {
					bytes = data.getLong("ifHCOutOctets") - oldData.getLong("ifHCOutOctets");
				}
				else if (data.has("ifOutOctets") && oldData.has("ifOutOctets")) {
					bytes = data.getLong("ifOutOctets") - oldData.getLong("ifOutOctets");
				}
				else {
					continue;
				}
				
				if (bytes >= 0) {
					bytes = bytes *8000 / interval;
					
					data.put("ifOutBPS", bytes);
					
					this.rollingMap.put(Resource.IFOUTOCTETS, index, bytes);
					
					max = Math.max(max, bytes);
					maxRate = Math.max(maxRate, bytes *100L / capacity);
				}
				
				if (this.critical != null) {
					this.critical.analyze(CriticalData.THROUGHPUT, index, capacity, max);
				}
			}
			
			this.agent.onSubmitTop(this.ip, "throughput", max);
			this.agent.onSubmitTop(this.ip, "throughputRate", maxRate);
			this.agent.onSubmitTop(this.ip, "throughputErr", maxErr);
		}
		
		lastRolling = super.data.getLong("lastResponse");
	}

	@Override
	protected void onFailure() {
		if (this.critical == null || !this.critical.monitorMode) {
			this.agent.onFailure(this.ip);
		}
	}
	
	@Override
	protected void onPending() {
		this.rollingMap.put(Resource.RESPONSETIME, "0", this.timeout);
		
		this.agent.onPending(this.ip);
	}
	
	class CriticalData {
	
		public static final String RESPONSETIME = "responseTime";
		public static final String PROCESSOR = "processor";
		public static final String MEMORY = "memory";
		public static final String STORAGE = "storage";
		public static final String THROUGHPUT = "throughput";
		
		private final Map<String, Critical> processor = new HashMap<String, Critical>();
		private final Map<String, Critical> storage = new HashMap<String, Critical>();
		private final Map<String, Critical> throughput = new HashMap<String, Critical>();
		
		public boolean monitorMode = false; 
		private Critical responseTime;
		
		public CriticalData(JSONObject criticalCondition) {
			JSONObject list;
			Map<String, Critical> mapping;
			String resource;
			
			for (Object key : criticalCondition.keySet()) {
				resource = (String)key;
				
				if (RESPONSETIME.equals(resource)) {
					JSONObject critical = criticalCondition.getJSONObject(RESPONSETIME).getJSONObject("0");
					
					monitorMode = (critical.getInt("limit") == 100);
					
					responseTime = new Critical(critical);
				}
				else {
					if (PROCESSOR.equals(resource)) {
						mapping = this.processor;
					}
					else if (MEMORY.equals(resource)) {
						mapping = this.storage;
					}
					else if (STORAGE.equals(resource)) {
						mapping = this.storage;				
					}
					else if (THROUGHPUT.equals(resource)) {
						mapping = this.throughput;
					}
					else {
						continue;
					}
					
					list = criticalCondition.getJSONObject(resource);
					
					for (Object index: list.keySet()) {
						mapping.put((String)index, new Critical(list.getJSONObject((String)index)));
					}
				}
			}
		}
		
		public void analyze(String resource, String index, long max, long current) {
			Map<String, Critical> mapping = null;
			Critical critical = null;
			
			if (RESPONSETIME.equals(resource)) {
				critical = this.responseTime;
			}
			else {
				if (PROCESSOR.equals(resource)) {
					mapping = this.processor;
					
					resource = "Processor load";
				}
				else if (MEMORY.equals(resource)) {
					mapping = this.storage;
					
					resource = "Physical memory";
				}
				else if (STORAGE.equals(resource)) {
					mapping = this.storage;
					
					resource = "Storage usage";
				}
				else if (THROUGHPUT.equals(resource)) {
					mapping = this.throughput;
					
					resource = "interface throughput";
				}
				
				if (mapping != null) {
					critical = mapping.get(index);
				}
			}
			
			if (critical != null) {
				long rate = current *100 / max;
				int value = critical.value(rate);
				
				if ((value & Critical.DIFF) > 0) {
					agent.onCritical(ip, (value & Critical.CRITIC) > 0
						, String.format("%s.%s %d%% %s", resource, index, rate, (value & Critical.CRITIC) > 0? " 성능 임계 초과.": " 성능 정상."));
				}
			}
		}
	}
	
	class Critical {
		
		public static final int DIFF = 0x01;
		public static final int CRITIC = 0x10;
		
		private final int limit;
		private Boolean status;
		
		public Critical(JSONObject criticalData) {
			this.limit = criticalData.getInt("limit");			
		}
		
		public int value(long current) {
			boolean critical = this.limit <= current;
			int value = critical? CRITIC: 0;
			
			if (this.status == null) {
				this.status = new Boolean(critical);
			}
			else {
				if (this.status != critical) {
					value |= DIFF;
				}
				
				this.status = critical;
			}
		
			return value;
		}
	}
}