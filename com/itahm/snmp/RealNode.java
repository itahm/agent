package com.itahm.snmp;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.Vector;

import org.json.JSONException;
import org.json.JSONObject;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Counter32;
import org.snmp4j.smi.Counter64;
import org.snmp4j.smi.Gauge32;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

import com.itahm.Data.Table;
import com.itahm.ITAhM;
import com.itahm.SnmpManager;
import com.itahm.json.JSONFile;
import com.itahm.json.RollingFile;
import com.itahm.json.RollingMap.Resource;
import com.itahm.json.RollingMap;

import com.itahm.event.Event;

public class RealNode extends CommunityTarget implements Node, Closeable {

	private static final long serialVersionUID = 7479923177197300424L;
	
	private final SnmpManager snmp;
	private final JSONFile file;
	private final JSONObject data;
	private TimerTask schedule;
	private String ip;
	private final JSONObject hrProcessorEntry;
	private JSONObject hrProcessorIndex;
	private final JSONObject ifEntry;
	private JSONObject ifIndex;
	private final JSONObject hrStorageEntry;
	private JSONObject hrStorageIndex;
	private RollingMap rollingMap;
	private final Address address = new Address();
	private final ArrayList<JSONObject> profileList = new ArrayList<JSONObject>();
	private final Map<String, Counter> inCounter = new HashMap<String, Counter>();
	private final Map<String, Counter> outCounter = new HashMap<String, Counter>();
	private final Map<String, Counter> hcInCounter = new HashMap<String, Counter>();
	private final Map<String, Counter> hcOutCounter = new HashMap<String, Counter>();
	
	private long requestTime;
	private boolean success = true;
	
	/**
	 * 기본 생성자
	 * @param snmp
	 * @throws IOException
	 */
	public RealNode(SnmpManager snmp) throws IOException {
		// target 설정
		setVersion(SnmpConstants.version2c);
		setTimeout(TIMEOUT);
		
		// file 및 json 초기화
		File nodeRoot = new File(new File(ITAhM.getRoot(), "snmp"), ip);
		nodeRoot.mkdirs();
		
		file = new JSONFile();
		file.load(new File(nodeRoot, "node"));
		data = file.getJSONObject();
		
		if (!data.has("hrProcessorEntry")) {
			data.put("hrProcessorEntry", hrProcessorEntry = new JSONObject());
		}
		else {
			hrProcessorEntry = data.getJSONObject("hrProcessorEntry");
		}
		
		if (!data.has("ifEntry")) {
			data.put("ifEntry", ifEntry = new JSONObject());
		}
		else {
			ifEntry = data.getJSONObject("ifEntry");
		}
		
		if (!data.has("ifIndex")) {
			data.put("ifIndex", ifIndex = new JSONObject());
		}
		else {
			ifIndex = data.getJSONObject("ifIndex");
		}
		
		if (!data.has("hrStorageEntry")) {
			data.put("hrStorageEntry", hrStorageEntry = new JSONObject());
		}
		else {
			hrStorageEntry = data.getJSONObject("hrStorageEntry");
		}
		
		if (!data.has("hrStorageIndex")) {
			data.put("hrStorageIndex", hrStorageIndex = new JSONObject());
		}
		else {
			hrStorageIndex = data.getJSONObject("hrStorageIndex");
		}
		
		file.save();
		
		// 기타 초기화
		this.snmp = snmp;

		hrProcessorIndex = new JSONObject();
		rollingMap = new RollingMap(nodeRoot);
	}
	
	/**
	 * 아직 등록되지 않은 node 생성자
	 * profile 목록 으로부터 자동으로 등록 시도 후 실패시 
	 * @param snmp
	 * @param ip
	 * @throws IOException
	 */
	public RealNode(SnmpManager snmp, String ip) throws IOException {
		this(snmp);
		
		// 기타 초기화
		this.ip = ip;
				
		// profile
		JSONObject profileData = Table.PROFILE.getJSONObject();
		String [] names = JSONObject.getNames(profileData);
		
		if (names != null) {
			for (int i=0, length=names.length; i< length; i++) {
				this.profileList.add(profileData.getJSONObject(names[i]));
			}
			
			trySNMP();
		}
	}
	
	public RealNode(SnmpManager snmp, String ip, String profileName) throws IOException {
		this(snmp);
		
		// 기타 초기화
		this.ip = ip;
		
		// profile
		try {
			JSONObject profile = Table.PROFILE.getJSONObject().getJSONObject(profileName);
			
			setAddress(new UdpAddress(String.format("%s/%d", ip, profile.getInt("udp"))));
			setCommunity(new OctetString(profile.getString("community")));
			
			RealNode node = this;
			
			snmp.scheduleAtFixedRate(new TimerTask() {

				@Override
				public void run() {
					try {
						snmp.request(node);
					} catch (IOException ioe) {
						ioe.printStackTrace();
					}
				}
				
			}, 0, 10000);
		}
		catch(JSONException jsone) {
		}
	}
	
	private void trySNMP() throws IOException {
		JSONObject profile = this.profileList.get(this.profileList.size() -1);
		
		try {
			setAddress(new UdpAddress(String.format("%s/%d", ip, profile.getInt("udp"))));
			setCommunity(new OctetString(profile.getString("community")));
			
			this.snmp.request(this);
		}
		catch(JSONException jsone) {
		}
	}
	
	public String get(String key) {
		try {
			return this.data.getString(key);
		}
		catch (JSONException jsone) {
			return null;
		}
	}
	
	@Override
	public void requestCompleted(boolean success) throws IOException {
		long responseTime = Calendar.getInstance().getTimeInMillis();
		long delay = responseTime - this.requestTime;
		
		if (success) {
			this.data.put("lastResponse", responseTime);
			this.data.put("delay", delay);
			this.data.put("timeout", -1);
			
			this.rollingMap.put(Resource.RESPONSETIME, "0", delay *100 /TIMEOUT);
		}
		else {
			this.data.put("timeout", responseTime);
		}
		
		if (this.success != success) {
			ITAhM.postMessage(new Event(this.data.getString("sysName"), this.ip, "snmp", success? 0: 1, success? 1: 0, ""));
		}
	}
	
	public void setRequestTime(long mills) {
		this.requestTime = mills;
	}
	
	/**
	 * Parse.
	 * 
	 * @param response
	 * @param variable
	 * @param reqest
	 * @return true get-next가 계속 진행되는 경우
	 * @throws IOException 
	 */
	public final boolean parse (OID response, Variable variable, OID request) throws IOException {
		if (request.startsWith(Constants.system)) {
			if (request.startsWith(Constants.sysDescr) && response.startsWith(Constants.sysDescr)) {
				OctetString value = (OctetString)variable;
				
				this.data.put("sysDescr", new String(value.getValue()));
			}
			else if (request.startsWith(Constants.sysObjectID) && response.startsWith(Constants.sysObjectID)) {
				OID value = (OID)variable;
				
				this.data.put("sysObjectID", value.toDottedString());
			}
			else if (request.startsWith(Constants.sysName) && response.startsWith(Constants.sysName)) {
				OctetString value = (OctetString)variable;
				
				this.data.put("sysName", new String(value.getValue()));
			}
		}
		else if (request.startsWith(Constants.ifEntry)) {
			JSONObject ifData;
			String index = Integer.toString(response.last());
			
			if (!this.ifEntry.has(index)) {
				this.ifEntry.put(index, ifData = new JSONObject());
			}
			else {
				ifData = this.ifEntry.getJSONObject(index);
			}
			
			if (request.startsWith(Constants.ifIndex)) {
				 if(response.startsWith(Constants.ifIndex)) {
					Integer32 value = (Integer32)variable;
					
					ifIndex.put(index, value.getValue());
					
					return true;
				 }
				 else {
					this.data.put("ifIndex", ifIndex);
						
					ifIndex = new JSONObject();
				 }
			}
			else if (request.startsWith(Constants.ifDescr) && response.startsWith(Constants.ifDescr)) {
				OctetString value = (OctetString)variable;
				
				ifData.put("ifDescr", new String(value.getValue()));
				
				return true;
			}
			else if (request.startsWith(Constants.ifType) && response.startsWith(Constants.ifType)) {
				Integer32 value = (Integer32)variable;
				
				ifData.put("ifType", value.getValue());
				
				return true;
			}
			else if (request.startsWith(Constants.ifSpeed) && response.startsWith(Constants.ifSpeed)) {
				Gauge32 value = (Gauge32)variable;
				
				ifData.put("ifSpeed", value.getValue());
				
				return true;
			}
			else if (request.startsWith(Constants.ifPhysAddress) && response.startsWith(Constants.ifPhysAddress)) {
				OctetString value = (OctetString)variable;
				
				ifData.put("ifPhysAddress", new String(value.getValue()));
				
				return true;
			}
			
			/**
			 * ifAdminStatus
			 * 1: "up"
			 * 2: "down",
			 * 3: "testing"
			 */
			else if (request.startsWith(Constants.ifAdminStatus) && response.startsWith(Constants.ifAdminStatus)) {
				Integer32 value = (Integer32)variable;
				int status = value.getValue();
				int last;
				
				if (ifData.has("ifAdminStatus")) {
					last = ifData.getInt("ifAdminStatus");
					
					if (status != last) {
						ITAhM.postMessage(new Event(this.data.getString("sysName"), this.ip, "ifAdminStatus", last, status, ""));
					}
				}
				
				ifData.put("ifAdminStatus", status);
				
				return true;
			}
			
			/**
			 * ifOperStatus
			 * 1: "up",
			 *	2: "down",
			 *	3: "testing",
			 *	4: "unknown",
			 *	5: "dormant",
			 *	6: "notPresent",
			 *	7: "lowerLayerDown"
			 */
			else if (request.startsWith(Constants.ifOperStatus) && response.startsWith(Constants.ifOperStatus)) {
				Integer32 value = (Integer32)variable;
				
				ifData.put("ifOperStatus", value.getValue());
				
				return true;
			}
			
			else if (request.startsWith(Constants.ifInOctets) && response.startsWith(Constants.ifInOctets)) {
				Counter32 value = (Counter32)variable;
				long longValue = value.getValue() *8;
				Counter inCounter = 	this.inCounter.get(index);
				
				if (inCounter == null) {
					this.inCounter.put(index, new Counter(longValue));
				}
				else {
					longValue = inCounter.count(longValue);
					
					this.rollingMap.put(Resource.IFINOCTETS, index, longValue);
					
					if (ifData.has("ifSpeed") && ifData.has("ifInOctets")) {
						long speed = ifData.getLong("ifSpeed");
						
						if (speed > 0) {
							long current = longValue *100 / speed;
							long last = ifData.getLong("ifInOctets") *100 / speed;
						
							if (current /10 != last /10 && (current > 69 || last > 69)) {
								ITAhM.postMessage(new Event(
										this.data.getString("sysName"),
										this.ip,
										"ifInOctets",
										index,
										last,
										current,
										""));
							}
						}
					}
					
					ifData.put("ifInOctets", longValue);
				}
				
				return true;
			}
			else if (request.startsWith(Constants.ifOutOctets) && response.startsWith(Constants.ifOutOctets)) {
				Counter32 value = (Counter32)variable;
				long longValue = value.getValue() *8;
				Counter outCounter = 	this.outCounter.get(index);
				
				if (outCounter == null) {
					this.outCounter.put(index, new Counter(longValue));
				}
				else {
					longValue = outCounter.count(longValue);
					
					this.rollingMap.put(Resource.IFOUTOCTETS, index, longValue);
					
					if (ifData.has("ifSpeed") && ifData.has("ifOutOctets")) {
						long speed = ifData.getLong("ifSpeed");
						
						if (speed > 0) {
							long current = longValue *100 / speed;
							long last = ifData.getLong("ifOutOctets") *100 / speed;
						
							if (current /10 != last /10 && (current > 69 || last > 69)) {
								ITAhM.postMessage(new Event(
										this.data.getString("sysName"),
										this.ip,
										"ifOutOctets",
										index,
										last,
										current,
										""));
							}
						}
					}
					
					ifData.put("ifOutOctets", longValue);
				}
				
				return true;
			}
		}
		else if (request.startsWith(Constants.ifXEntry)) {
			JSONObject ifData;
			String index = Integer.toString(response.last());
			
			if (!this.ifEntry.has(index)) {
				this.ifEntry.put(index, ifData = new JSONObject());
			}
			else {
				ifData = this.ifEntry.getJSONObject(index);
			}
			
			if (request.startsWith(Constants.ifName) && response.startsWith(Constants.ifName)) {
				OctetString value = (OctetString)variable;
				
				ifData.put("ifName", new String(value.getValue()));
				
				return true;
			}
			else if (request.startsWith(Constants.ifAlias) && response.startsWith(Constants.ifAlias)) {
				OctetString value = (OctetString)variable;
				
				ifData.put("ifAlias", new String(value.getValue()));
				
				return true;
			}
			else if (request.startsWith(Constants.ifHCInOctets) && response.startsWith(Constants.ifHCInOctets)) {
				Counter64 value = (Counter64)variable;
				long longValue = value.getValue() *8;
				Counter hcInCounter = 	this.hcInCounter.get(index);
				
				if (hcInCounter == null) {
					this.hcInCounter.put(index, new Counter(longValue));
				}
				else {
					longValue = hcInCounter.count(longValue);
					
					this.rollingMap.put(Resource.IFINOCTETS, index, longValue);
					
					if (ifData.has("ifSpeed") && ifData.has("ifHCInOctets")) {
						long speed = ifData.getLong("ifSpeed");
						
						if (speed > 0) {
							long current = longValue *100 / speed;
							long last = ifData.getLong("ifHCInOctets") *100 / speed;
							
							if (current /10 != last /10 && (current > 69 || last > 69)) {
								ITAhM.postMessage(new Event(
										this.data.getString("sysName"),
										this.ip,
										"ifInOctets",
										index,
										last,
										current,
										""));
							}
						}
					}
					
					ifData.put("ifHCInOctets", longValue);
				}
				
				return true;
			}
			else if (request.startsWith(Constants.ifHCOutOctets) && response.startsWith(Constants.ifHCOutOctets)) {
				Counter64 value = (Counter64)variable;
				long longValue = value.getValue() *8;
				Counter hcOutCounter = 	this.hcOutCounter.get(index);
				
				if (hcOutCounter == null) {
					this.hcOutCounter.put(index, new Counter(longValue));
				}
				else {
					longValue = hcOutCounter.count(longValue);
					
					this.rollingMap.put(Resource.IFOUTOCTETS, index, longValue);
				
					if (ifData.has("ifSpeed") && ifData.has("ifHCOutOctets")) {
						long speed = ifData.getLong("ifSpeed");
						
						if (speed > 0) {
							long current = longValue *100 / speed;
							long last = ifData.getLong("ifHCOutOctets") *100 / speed;
							
							if (current /10 != last /10 && (current > 69 || last > 69)) {
								ITAhM.postMessage(new Event(
										this.data.getString("sysName"),
										this.ip,
										"ifOutOctets",
										index,
										last,
										current,
										""));
							}
						}
					}
					
					ifData.put("ifHCOutOctets", longValue);
				}
				
				return true;
			}
		}
		else if (request.startsWith(Constants.ipNetToMediaTable)) {
			int [] array = response.getValue();
			int size = array.length;
			
			String ip = String.format("%d.%d.%d.%d", array[size -4], array[size -3], array[size -2], array[size -1]);
			
			if (request.startsWith(Constants.ipNetToMediaType) && response.startsWith(Constants.ipNetToMediaType)) {
				Integer32 value = (Integer32)variable;
				
				if (value.getValue() == 3) {
					address.put(ip);
				}
				
				return true;
			}
			else if (request.startsWith(Constants.ipNetToMediaPhysAddress) && response.startsWith(Constants.ipNetToMediaPhysAddress)) {
				OctetString value = (OctetString)variable;
				byte [] mac = value.getValue();
				if (mac.length == 6) {
					address.put(ip, String.format("%02X-%02X-%02X-%02X-%02X-%02X", mac[0] & 0xff, mac[1] & 0xff, mac[2] & 0xff, mac[3] & 0xff, mac[4] & 0xff, mac[5] & 0xff));
				}
				
				return true;
			}
		}
		else if (request.startsWith(Constants.host)) {
			if (request.startsWith(Constants.hrSystemUptime) && response.startsWith(Constants.hrSystemUptime)) {
				TimeTicks value = (TimeTicks)variable; 
				
				this.data.put("hrSystemUptime", value.toMilliseconds());
			}
			else if (request.startsWith(Constants.hrProcessorLoad)) {
				if (response.startsWith(Constants.hrProcessorLoad)) {
					Integer32 value = (Integer32)variable;
					String index = Integer.toString(response.last());
					int intValue = value.getValue();
					
					this.rollingMap.put(Resource.HRPROCESSORLOAD, index, intValue);
					
					if (this.hrProcessorEntry.has(index)) {
						int last = this.hrProcessorEntry.getInt(index);
						
						if (intValue /10 != last /10 && (intValue > 69 || last > 69)) {
							ITAhM.postMessage(new Event(
										this.data.getString("sysName"),
										this.ip,
										"hrProcessorLoad",
										index,
										last,
										intValue,
										""));
						}
					}
		
					this.hrProcessorEntry.put(index, intValue);
					
					this.hrProcessorIndex.put(index, Integer.parseInt(index, 10));
					
					return true;
				}
				else {
					this.data.put("hrProcessorIndex", this.hrProcessorIndex);
					
					this.hrProcessorIndex = new JSONObject();
				}
			}
			else if (request.startsWith(Constants.hrStorageEntry) && response.startsWith(Constants.hrStorageEntry)) {
				JSONObject storageData;
				String index = Integer.toString(response.last());
				
				if (!this.hrStorageEntry.has(index)) {
					this.hrStorageEntry.put(index, storageData = new JSONObject());
				}
				else {
					storageData = this.hrStorageEntry.getJSONObject(index);
				}
				
				if (request.startsWith(Constants.hrStorageIndex)) {
					if (response.startsWith(Constants.hrStorageIndex)) {
						Integer32 value = (Integer32)variable;
						
						hrStorageIndex.put(index, value.getValue());
						
						return true;
					}
					else {
						this.data.put("hrStorageIndex", hrStorageIndex);
						
						hrStorageIndex = new JSONObject();
					}
				}
				else if (request.startsWith(Constants.hrStorageType) && response.startsWith(Constants.hrStorageType)) {
					OID value = (OID)variable;
					
					if (value.startsWith(Constants.hrStorageTypes)) {
						storageData.put("hrStorageType", value.last());
					}
					
					return true;
				}
				else if (request.startsWith(Constants.hrStorageDescr) && response.startsWith(Constants.hrStorageDescr)) {
					OctetString value = (OctetString)variable;
					
					storageData.put("hrStorageDescr", new String(value.getValue()));
					
					return true;
				}
				else if (request.startsWith(Constants.hrStorageAllocationUnits) && response.startsWith(Constants.hrStorageAllocationUnits)) {
					Integer32 value = (Integer32)variable;
					
					storageData.put("hrStorageAllocationUnits", value.getValue());
					
					return true;
				}
				else if (request.startsWith(Constants.hrStorageSize) && response.startsWith(Constants.hrStorageSize)) {
					Integer32 value = (Integer32)variable;
					
					storageData.put("hrStorageSize", value.getValue());
					
					return true;
				}
				else if (request.startsWith(Constants.hrStorageUsed) && response.startsWith(Constants.hrStorageUsed)) {
					Integer32 value = (Integer32)variable;
					int intValue = value.getValue();
					
					if (storageData.has("hrStorageAllocationUnits")) {
						this.rollingMap.put(Resource.HRSTORAGEUSED, index, 1L* intValue * storageData.getInt("hrStorageAllocationUnits"));
					}
					
					
					if (storageData.has("hrStorageUsed") && storageData.has("hrStorageSize") && storageData.has("hrStorageType")) {
						int size = storageData.getInt("hrStorageSize");
						
						if (size != 0) {
							int last = storageData.getInt("hrStorageUsed") / size;
							int type = storageData.getInt("hrStorageType");
							
							intValue /= size;
							if (intValue /10 != last /10 && (intValue > 69 || last > 69)) {
								ITAhM.postMessage(new Event(
										this.data.getString("sysName"),
										this.ip,
										"hrStorageUsed/"+ type,
										index,
										last,
										intValue,
										""));
							}
						}
					}
					
					storageData.put("hrStorageUsed", intValue);
					
					return true;
				}
			}
		}
		
		return false;
	}

	public final PDU parse(PDU request, PDU response) throws IOException {
		Vector<? extends VariableBinding> requestVBs = request.getVariableBindings();
		Vector<? extends VariableBinding> responseVBs = response.getVariableBindings();
		Vector<VariableBinding> nextRequests = new Vector<VariableBinding>();
		VariableBinding requestVB, responseVB;
		
		for (int i=0, length = responseVBs.size(); i<length; i++) {
			requestVB = (VariableBinding)requestVBs.get(i);
			responseVB = (VariableBinding)responseVBs.get(i);
			try {
			if (parse(responseVB.getOid(), responseVB.getVariable(), requestVB.getOid())) {
				if (!responseVB.equals(Null.endOfMibView)) {
					nextRequests.add(responseVB);
				}
				else {
					System.out.println("end of mib view.");
				}
			}
			}
			catch(Exception jsone) {
				jsone.printStackTrace();
			}
		}
		
		return nextRequests.size() > 0? new PDU(PDU.GETNEXT, nextRequests): null;
	}
	
	public String getIP() {
		return this.ip;
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
		}
		
		return data;
	}
	
	@Override
	public void close() {
		if (this.schedule != null) {
			this.schedule.cancel();
		}
	}
	
}
