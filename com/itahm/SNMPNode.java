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

	private static final long TIMEOUT = 5000;
	
	private final RollingMap rollingMap;
	private final String ip;
	private final SNMPAgent agent;
	private long lastRolling;
	private CriticalData critical;
	
	public SNMPNode(SNMPAgent agent, String ip, int udp, String community, JSONObject criticalCondition) throws IOException {
		super(agent, ip, udp, community, TIMEOUT);
		
		this.agent = agent;
		this.ip = ip;
		
		File nodeRoot = new File(new File(ITAhM.getRoot(), Constant.STRING_SNMP), ip);
		nodeRoot.mkdirs();
		
		rollingMap = new RollingMap(nodeRoot);
		
		this.critical = new CriticalData(criticalCondition);
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
			String index = String.valueOf(arpTable.getInt(mac));
			
			return ifEntry.getJSONObject(index).getString(Constant.STRING_IFNAME);
		}
		
		return null;
	}
	
	public String getPeerIFName(SNMPNode peer){
		JSONObject ifEntry = this.data.getJSONObject(Constant.STRING_IFENTRY);
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
	
	private void parseResponse() {
		JSONObject data;
		JSONObject oldData;
		long max;
		long maxRate;
		long value;
		long capacity;
		long tmpValue;
		
		this.agent.onSuccess(ip);
		
		this.rollingMap.put(Resource.RESPONSETIME, "0", super.responseTime);
		
		this.critical.analyze(CriticalData.RESPONSETIME, "0", TIMEOUT, super.responseTime);
		
		this.agent.onSubmitTop(ip, "responseTime", super.responseTime);
		
		max = 0;
		for(String index: super.hrProcessorEntry.keySet()) {
			value = super.hrProcessorEntry.get(index);
			
			this.rollingMap.put(Resource.HRPROCESSORLOAD, index, value);
			
			this.critical.analyze(CriticalData.PROCESSOR, index, 100, value);
			
			max = Math.max(max, value);
		}
		
		this.agent.onSubmitTop(ip, "processor", max);
		
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
				this.critical.analyze(CriticalData.MEMORY, index, capacity, tmpValue);
				
				this.agent.onSubmitTop(ip, "memory", value);
				this.agent.onSubmitTop(ip, "memoryRate", tmpValue *100L / capacity);
				
				break;
			case 4:
				this.critical.analyze(CriticalData.STORAGE, index, capacity, tmpValue);
				
				max = Math.max(max, value);
				maxRate = Math.max(maxRate, tmpValue *100L / capacity);
			}
		}
		
		this.agent.onSubmitTop(ip, "storage", max);
		this.agent.onSubmitTop(ip, "storageRate", maxRate);
		
		if (lastRolling > 0) {
			long interval = super.data.getLong("lastResponse") - lastRolling;
			JSONObject ifEntry = super.data.getJSONObject("ifEntry");
			long bytes;
			long in;
			long out;
			
			max = 0;
			maxRate = 0;
			for(String index: super.ifEntry.keySet()) {
				data = super.ifEntry.get(index);
				
				capacity = data.getInt("ifSpeed");
				
				if (!ifEntry.has(index) || capacity <= 0) {
					continue;
				}
				
				oldData = ifEntry.getJSONObject(index);
				
				if (data.has(Constant.STRING_IFHCOUT)) {
					bytes = data.getLong(Constant.STRING_IFHCOUT);
				}
				else {
					bytes = data.getLong(Constant.STRING_IFOUT);
				}
				
				if (oldData.has(Constant.STRING_IFHCOUT)) {
					bytes -= oldData.getLong(Constant.STRING_IFHCOUT);
				}
				else {
					bytes -= oldData.getLong(Constant.STRING_IFOUT);
				}
				
				in = bytes *8000 / interval;
				
				data.put("ifOutBPS", in);
				
				this.rollingMap.put(Resource.IFOUTOCTETS, index, in);
				
				max = Math.max(max, in);
				maxRate = Math.max(maxRate, in *100L / capacity);
				
				if (data.has(Constant.STRING_IFHCIN)) {
					bytes = data.getLong(Constant.STRING_IFHCIN);
				}
				else {
					bytes = data.getLong(Constant.STRING_IFIN);
				}
				
				if (oldData.has(Constant.STRING_IFHCIN)) {
					bytes -= oldData.getLong(Constant.STRING_IFHCIN);
				}
				else {
					bytes -= oldData.getLong(Constant.STRING_IFIN);
				}
				
				out = bytes *8000 / interval;
				
				data.put("ifInBPS", out);
				
				this.rollingMap.put(Resource.IFINOCTETS, index, out);
				
				max = Math.max(max, out);
				maxRate = Math.max(maxRate, out *100L / capacity);
				
				this.critical.analyze(CriticalData.THROUGHPUT, index, capacity, Math.max(out, in));
			}
			
			this.agent.onSubmitTop(ip, "throughput", max);
			this.agent.onSubmitTop(ip, "throughputRate", maxRate);
		}
	}
	@Override
	public void onSuccess() {
		try {
			parseResponse();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		lastRolling = this.data.getLong("lastResponse");
	}

	@Override
	protected void onFailure() {
		this.agent.onFailure(ip);
	}

	class CriticalData {
	
		public static final String RESPONSETIME = "responseTime";
		public static final String PROCESSOR = "processor";
		public static final String MEMORY = "memory";
		public static final String STORAGE = "storage";
		public static final String THROUGHPUT = "throughput";
		
		private final Map<String, Critical> processor;
		private final Map<String, Critical> storage;
		private final Map<String, Critical> throughput;
		private Critical responseTime;
		
		public CriticalData(JSONObject criticalCondition) {			
			this.processor = new HashMap<String, Critical>();
			this.storage = new HashMap<String, Critical>();
			this.throughput = new HashMap<String, Critical>();
			
			if (criticalCondition == null) {
				return;
			}
			
			JSONObject list;
			Map<String, Critical> mapping;
			String resource;
			
			for (Object key : criticalCondition.keySet()) {
				resource = (String)key;
				
				if (RESPONSETIME.equals(resource)) {
					responseTime = new Critical(criticalCondition.getJSONObject(RESPONSETIME).getJSONObject("0"));
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
						, String.format("%s index[%s] %d%% %s", resource, index, rate, (value & Critical.CRITIC) > 0? " 성능 임계 초과.": " 성능 정상."));
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