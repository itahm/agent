package com.itahm.snmp;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.json.JSONObject;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
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

import com.itahm.ITAhM;
import com.itahm.SnmpManager;
import com.itahm.json.JSONFile;
import com.itahm.json.RollingFile;
import com.itahm.json.RollingMap.Resource;
import com.itahm.json.RollingMap;

public class Node extends CommunityTarget implements ResponseListener {

	private static final long serialVersionUID = 7479923177197300424L;
	
	private static final long TIMEOUT = 5000;
	private static final String STRING_SNMP = "snmp";
	private static final String STRING_NODE = "node";
	
	private final JSONFile file;
	private final JSONObject data;
	private final UdpAddress address;
	private ArrayList<String> testList; 
	private final JSONObject hrProcessorEntry;
	private JSONObject hrProcessorIndex;
	private final JSONObject ifEntry;
	private JSONObject ifIndex;
	private final JSONObject hrStorageEntry;
	private JSONObject hrStorageIndex;
	private RollingMap rollingMap;
	private final Address addressEntry = new Address();
	private final Map<String, Counter> inCounter = new HashMap<String, Counter>();
	private final Map<String, Counter> outCounter = new HashMap<String, Counter>();
	private final Map<String, Counter> hcInCounter = new HashMap<String, Counter>();
	private final Map<String, Counter> hcOutCounter = new HashMap<String, Counter>();
	
	public long requestTime;
	
	/**
	 * 기본 생성자
	 * @param snmp
	 * @throws IOException
	 */
	public Node(String ip) throws IOException {
		// target 설정
		setVersion(SnmpConstants.version2c);
		setTimeout(TIMEOUT);
		
		address = new UdpAddress();
		address.setInetAddress(InetAddress.getByName(ip));
		
		// file 및 json 초기화
		File nodeRoot = new File(new File(ITAhM.getRoot(), STRING_SNMP), ip);
		nodeRoot.mkdirs();
		
		file = new JSONFile(new File(nodeRoot, STRING_NODE));
		
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
		
		if (!data.has("hrStorageEntry")) {
			data.put("hrStorageEntry", hrStorageEntry = new JSONObject());
		}
		else {
			hrStorageEntry = data.getJSONObject("hrStorageEntry");
		}
		
		file.save();
		
		// 기타 초기화
		
		hrProcessorIndex = new JSONObject();
		hrStorageIndex = new JSONObject();
		ifIndex = new JSONObject();
		
		rollingMap = new RollingMap(nodeRoot);
	}

	public void test (ArrayList<String> profileList) {
		this.testList = profileList;
	}
	
	public ArrayList<String> test () {
		return this.testList;
	}
	
	public void set(String community, int udp) {
		this.address.setPort(udp);
		
		setAddress(this.address);
		//setAddress(new UdpAddress(String.format("%s/%d", this.ip, udp)));
		setCommunity(new OctetString(community));
	}
	
	public Node put (String key, String value) {
		this.data.put(key, value);
		
		try {
			this.file.save();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return this;
	}
	
	public void requestCompleted() throws IOException {
		long currentTime = Calendar.getInstance().getTimeInMillis();
		long responseTime = currentTime - this.requestTime;
		
		this.data.put("lastResponse", currentTime);
		this.data.put("responseTime", responseTime);
		
		this.rollingMap.put(Resource.RESPONSETIME, "0", responseTime);
			
		file.save();
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
	private final boolean parse (OID response, Variable variable, OID request) throws IOException {
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
				
				ifData.put("ifAdminStatus", value.getValue());
				
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
					addressEntry.put(ip);
				}
				
				return true;
			}
			else if (request.startsWith(Constants.ipNetToMediaPhysAddress) && response.startsWith(Constants.ipNetToMediaPhysAddress)) {
				OctetString value = (OctetString)variable;
				byte [] mac = value.getValue();
				if (mac.length == 6) {
					addressEntry.put(ip, String.format("%02X-%02X-%02X-%02X-%02X-%02X", mac[0] & 0xff, mac[1] & 0xff, mac[2] & 0xff, mac[3] & 0xff, mac[4] & 0xff, mac[5] & 0xff));
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
					
					this.hrProcessorEntry.put(index, intValue);
					this.hrProcessorIndex.put(index, Integer.parseInt(index));
					
					return true;
				}
				else {
					this.data.put("hrProcessorIndex", hrProcessorIndex);
					
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
					
					storageData.put("hrStorageUsed", intValue);
					
					return true;
				}
			}
		}
		
		return false;
	}

	public final PDU getNextRequest(PDU request, PDU response) throws IOException {
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
	
	public JSONObject getData() {
		return this.data;
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
	public void onResponse(ResponseEvent event) {
		PDU request = event.getRequest();
		PDU response = event.getResponse();
		SnmpManager snmp = ((SnmpManager)event.getUserObject());
		
		snmp.cancel(request, this);
		
		// request 보냈다는것은 snmp true 이거나 notfound (테스트)
		// snmp false 에서는 request 하지 않음.
		if (response == null) {
			// response timed out
			snmp.onFailure(this);
			
			return;
		}
		
		int status = response.getErrorStatus();
		
		if (status == PDU.noError) {
			try {
				PDU nextRequest = getNextRequest(request, response);
				
				if (nextRequest == null) {
					// end of get-next request
					snmp.onSuccess(this);
				}
				else {
					snmp.sendNextRequest(this, nextRequest);
				}
			} catch (IOException e) {
				// TODO fatal error
				e.printStackTrace();
			}
		}
		else {
			new Exception().printStackTrace();
		}
	}	
}
