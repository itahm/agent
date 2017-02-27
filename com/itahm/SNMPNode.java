package com.itahm;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;

import com.itahm.json.JSONException;
import com.itahm.json.JSONObject;
import com.itahm.icmp.ICMPListener;
import com.itahm.json.RollingFile;
import com.itahm.snmp.Node;
import com.itahm.table.Table;

public class SNMPNode extends Node implements ICMPListener, Closeable {
	
	private final static OID OID_TRAP = new OID(new int [] {1,3,6,1,6,3,1,1,5});
	private final static OID OID_LINKDOWN = new OID(new int [] {1,3,6,1,6,3,1,1,5,3});
	private final static OID OID_LINKUP = new OID(new int [] {1,3,6,1,6,3,1,1,5,4});
	
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
	
	private File nodeRoot;
	private final Map<Rolling, HashMap<String, RollingFile>> rollingMap = new HashMap<Rolling, HashMap<String, RollingFile>>();
	private String ip;
	private SNMPAgent agent;
	private ICMPNode icmp;
	private long responseTime;
	private long lastRolling = 0;
	private Critical critical;
	
	public static SNMPNode getInstance(SNMPAgent agent, String ip, int udp, String user, int level, JSONObject criticalCondition) throws IOException {
		SNMPNode node = new SNMPNode(agent, ip, udp, user, level, criticalCondition);
		
		node.initialize(agent, ip, criticalCondition);
		
		return node;
	}
	
	public static SNMPNode getInstance(SNMPAgent agent, String ip, int udp, String community, JSONObject criticalCondition) throws IOException {
		SNMPNode node = new SNMPNode(agent, ip, udp, community, criticalCondition);
		
		node.initialize(agent, ip, criticalCondition);
		
		return node;
	}
	
	private SNMPNode(SNMPAgent agent, String ip, int udp, String community, JSONObject criticalCondition) throws IOException {
		super(agent, ip, udp, new OctetString(community), Agent.MAX_TIMEOUT);
		
		agent.setRequestOID(super.pdu);
	}
	
	private SNMPNode(SNMPAgent agent, String ip, int udp, String user, int level, JSONObject criticalCondition) throws IOException {
		super(agent, ip, udp, new OctetString(user), level, Agent.MAX_TIMEOUT);
		
		agent.setRequestOID(super.pdu);
	}
	
	private void initialize(SNMPAgent agent, String ip, JSONObject criticalCondition) throws UnknownHostException {
		this.agent = agent;
		this.ip = ip;
		
		this.icmp = new ICMPNode(this, ip);
		
		this.nodeRoot = new File(agent.nodeRoot, ip);
		this.nodeRoot.mkdirs();
		
		for (Rolling database : Rolling.values()) {
			rollingMap.put(database, new HashMap<String, RollingFile>());
			
			new File(nodeRoot, database.toString()).mkdir();
		}
		
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
	
	private void parseNetwork() {
		for (String network : super.networkTable.keySet()) {
			if ("127.0.0.1".equals(network)) {
				continue;
			}
			
			this.agent.onNetwork(network, super.networkTable.get(network));
		}
	}
	
	private void parseARP() {
		Integer index;
		for (String mac : super.arpTable.keySet()) {
			index = super.macTable.get(mac);
			
			if (index != null) {
				this.agent.onARP(mac, super.arpTable.get(mac), super.maskTable.get(index));
			}
		}
	}
	
	private void parseResponseTime() {
		putData(Rolling.RESPONSETIME, "0", this.responseTime);
		
		this.agent.onSubmitTop(this.ip, SNMPAgent.Resource.RESPONSETIME, this.responseTime);
	}
	
	private void parseProcessor() {
		long max = 0;
		long value;
		
		for(String index: super.hrProcessorEntry.keySet()) {
			value = super.hrProcessorEntry.get(index);
			
			putData(Rolling.HRPROCESSORLOAD, index, value);
			
			if (this.critical != null) {
				this.critical.analyze(Critical.Resource.PROCESSOR, index, 100, value);
			}
			
			max = Math.max(max, value);
		}
	
		this.agent.onSubmitTop(this.ip, SNMPAgent.Resource.PROCESSOR, max);
	}
	
	private void parseStorage() {
		JSONObject data;
		long max = 0;
		long maxRate = 0;
		long value;
		long capacity;
		long tmpValue;
		int type;
		
		for(String index: super.hrStorageEntry.keySet()) {
			data = super.hrStorageEntry.get(index);
			
			try {
				capacity = data.getInt("hrStorageSize");
				tmpValue = data.getInt("hrStorageUsed");
				value = 1L* tmpValue * data.getInt("hrStorageAllocationUnits");
				type = data.getInt("hrStorageType");
			} catch (JSONException jsone) {
				System.out.println(jsone);
				
				return;
			}
			
			if (capacity <= 0) {
				continue;
			}
			
			putData(Rolling.HRSTORAGEUSED, index, value);
			
			switch(type) {
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
	}
	
	private void parseInterface(JSONObject ifEntry) {
		JSONObject device = Agent.getTable(Table.DEVICE).getJSONObject(this.ip);
		JSONObject ifSpeed = device.has("ifSpeed")? device.getJSONObject("ifSpeed"): null;
		JSONObject data;
		JSONObject oldData;
		long value;
		long capacity;
		long bytes;
		long max = 0;
		long maxRate = 0;
		long maxErr = 0;
		
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
	
	private void processSuccess() {		
		parseNetwork();
		
		parseARP();
	
		parseResponseTime();
		
		parseProcessor();
		
		parseStorage();
		
		if (super.data.has("ifEntry")) {
			parseInterface(super.data.getJSONObject("ifEntry"));
		}		
	}
	
	public void parseTrap(OID trap, Variable variable) {
		if (trap.startsWith(OID_TRAP)) {
			if (trap.startsWith(OID_LINKUP)) {
				
			}
			else if (trap.startsWith(OID_LINKDOWN)) {
				
			}
		}
	}
	
	@Override
	public void onSuccess() {
		processSuccess();
		
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
		
		this.agent.onSuccess(this.ip);
	}

	@Override
	public void onFailure(String host) {
		this.agent.onFailure(this.ip);
	}

	@Override
	public void onException() {
		this.agent.onException(this.ip);
	}
	
	@Override
	public void close() throws IOException {
		this.icmp.stop();
	}

	@Override
	protected boolean parseEnterprise(OID response, Variable variable, OID request) {
		return this.agent.parseEnterprise(this, response, variable, request);
	}
	
}