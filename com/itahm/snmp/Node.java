package com.itahm.snmp;

import java.io.IOException;

import java.net.InetAddress;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.json.JSONException;
import org.json.JSONObject;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
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
import org.snmp4j.transport.DefaultUdpTransportMapping;

import com.itahm.Constant;
import com.itahm.ITAhM;

public abstract class Node implements ResponseListener {
	
	private final Snmp snmp;
	private final CommunityTarget target;
	private long requestTime;
	protected long responseTime;
	private boolean completed;
	
	/**
	 * 이전 데이터 보관소
	 */
	protected final JSONObject data;
	
	/**
	 * 최신 데이터 보관소
	 */
	protected Map<String, Integer> hrProcessorEntry;
	protected Map<String, JSONObject> hrStorageEntry;
	protected Map<String, JSONObject> ifEntry;
	protected Map<String, Integer> arpTable;
	
	public Node(Snmp snmp, String ip, int udp, String community, long timeout) throws IOException {
		this.snmp = snmp;
		
		data = new JSONObject();
		
		completed = true;
		
		// target 설정
		target = new CommunityTarget(new UdpAddress(InetAddress.getByName(ip), udp), new OctetString(community));
		target.setVersion(SnmpConstants.version2c);
		target.setTimeout(timeout);
	}
	
	public void request (PDU pdu) {
		if (!completed) {
			ITAhM.debug("delay");
			
			return;
		}
		
		if (!this.completed) {
			System.out.println("delay");
		}
		
		this.completed = false;
		
		// 존재하지 않는 index 지워주기 위해 초기화
		hrProcessorEntry = new HashMap<String, Integer>();
		hrStorageEntry = new HashMap<String, JSONObject>();
		ifEntry = new HashMap<String, JSONObject>();
		arpTable = new HashMap<String, Integer>();
		
		this.requestTime = Calendar.getInstance().getTimeInMillis();
		
		sendRequest(pdu);
	}
	
	public JSONObject getData() {		
		return this.data;
	}
	
	private final void sendRequest(PDU pdu) {
		try {
			this.snmp.send(pdu, this.target, null, this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private final boolean parseSystem(OID response, Variable variable, OID request) {
		if (request.startsWith(RequestPDU.sysDescr) && response.startsWith(RequestPDU.sysDescr)) {
			this.data.put("sysDescr", new String(((OctetString)variable).getValue()));
		}
		else if (request.startsWith(RequestPDU.sysObjectID) && response.startsWith(RequestPDU.sysObjectID)) {
			this.data.put("sysObjectID", ((OID)variable).toDottedString());
		}
		else if (request.startsWith(RequestPDU.sysName) && response.startsWith(RequestPDU.sysName)) {
			this.data.put("sysName", new String(((OctetString)variable).getValue()));
		}
		
		return false;
	}
	
	private final boolean parseIFEntry(OID response, Variable variable, OID request) throws IOException {
		String index = Integer.toString(response.last());
		JSONObject ifData = this.ifEntry.get(index);
		
		if(ifData == null) {
			ifData = new JSONObject();
					
			this.ifEntry.put(index, ifData);
			
			ifData.put("ifInBPS", 0);
			ifData.put("ifOutBPS", 0);
		}
		
		if (request.startsWith(RequestPDU.ifDescr) && response.startsWith(RequestPDU.ifDescr)) {
			ifData.put("ifDescr", new String(((OctetString)variable).getValue()));
		}
		else if (request.startsWith(RequestPDU.ifType) && response.startsWith(RequestPDU.ifType)) {			
			ifData.put("ifType", ((Integer32)variable).getValue());
		}
		else if (request.startsWith(RequestPDU.ifSpeed) && response.startsWith(RequestPDU.ifSpeed)) {			
			ifData.put("ifSpeed", ((Gauge32)variable).getValue());
		}
		else if (request.startsWith(RequestPDU.ifPhysAddress) && response.startsWith(RequestPDU.ifPhysAddress)) {
			ifData.put(Constant.STRING_MAC_ADDR, new String(((OctetString)variable).getValue()));
		}
		else if (request.startsWith(RequestPDU.ifAdminStatus) && response.startsWith(RequestPDU.ifAdminStatus)) {
			ifData.put(Constant.STRING_IFADMINSTAT, ((Integer32)variable).getValue());
		}
		else if (request.startsWith(RequestPDU.ifOperStatus) && response.startsWith(RequestPDU.ifOperStatus)) {			
			ifData.put("ifOperStatus", ((Integer32)variable).getValue());
		}
		else if (request.startsWith(RequestPDU.ifInOctets) && response.startsWith(RequestPDU.ifInOctets)) {
			ifData.put("ifInOctets", ((Counter32)variable).getValue());
		}
		else if (request.startsWith(RequestPDU.ifOutOctets) && response.startsWith(RequestPDU.ifOutOctets)) {
			ifData.put("ifOutOctets", ((Counter32)variable).getValue());
		}
		else if (request.startsWith(RequestPDU.ifInErrors) && response.startsWith(RequestPDU.ifInErrors)) {
			ifData.put("ifInErrors", ((Counter32)variable).getValue());
		}
		else if (request.startsWith(RequestPDU.ifOutErrors) && response.startsWith(RequestPDU.ifOutErrors)) {
			ifData.put("ifOutErrors", ((Counter32)variable).getValue());
		}
		else {
			return false;
		}
		
		return true;
	}
	
	private final boolean parseIFXEntry(OID response, Variable variable, OID request) throws IOException {
		String index = Integer.toString(response.last());
		JSONObject ifData = this.ifEntry.get(index);
		
		if(ifData == null) {
			ifData = new JSONObject();
			
			this.ifEntry.put(index, ifData);
		}
		
		if (request.startsWith(RequestPDU.ifName) && response.startsWith(RequestPDU.ifName)) {
			ifData.put(Constant.STRING_IFNAME, new String(((OctetString)variable).getValue()));
		}
		else if (request.startsWith(RequestPDU.ifAlias) && response.startsWith(RequestPDU.ifAlias)) {
			ifData.put(Constant.STRING_IFALIAS, new String(((OctetString)variable).getValue()));
		}
		else if (request.startsWith(RequestPDU.ifHCInOctets) && response.startsWith(RequestPDU.ifHCInOctets)) {
			ifData.put("ifHCInOctets", ((Counter64)variable).getValue());
		}
		else if (request.startsWith(RequestPDU.ifHCOutOctets) && response.startsWith(RequestPDU.ifHCOutOctets)) {
			ifData.put("ifHCOutOctets", ((Counter64)variable).getValue());
		}
		else if (request.startsWith(RequestPDU.ifHighSpeed) && response.startsWith(RequestPDU.ifHighSpeed)) {
			ifData.put("ifHighSpeed", ((Gauge32)variable).getValue() * 1000000L);
		}
		else {
			return false;
		}
		
		return true;
	}
	
	private final boolean parseHost(OID response, Variable variable, OID request) throws JSONException, IOException {
		if (request.startsWith(RequestPDU.hrSystemUptime) && response.startsWith(RequestPDU.hrSystemUptime)) {
			this.data.put(Constant.STRING_SYSUPTIME, ((TimeTicks)variable).toMilliseconds());
			
			return false;
		}
		
		String index = Integer.toString(response.last());
		
		if (request.startsWith(RequestPDU.hrProcessorLoad) && response.startsWith(RequestPDU.hrProcessorLoad)) {
			this.hrProcessorEntry.put(index, ((Integer32)variable).getValue());
		}
		else if (request.startsWith(RequestPDU.hrStorageEntry) && response.startsWith(RequestPDU.hrStorageEntry)) {
			JSONObject storageData = this.hrStorageEntry.get(index);
			
			if (storageData == null) {
				storageData = new JSONObject();
				
				this.hrStorageEntry.put(index, storageData = new JSONObject());
			}
			
			if (request.startsWith(RequestPDU.hrStorageType) && response.startsWith(RequestPDU.hrStorageType)) {
				storageData.put("hrStorageType", ((OID)variable).last());
			}
			else if (request.startsWith(RequestPDU.hrStorageDescr) && response.startsWith(RequestPDU.hrStorageDescr)) {
				storageData.put("hrStorageDescr", new String(((OctetString)variable).getValue()));
			}
			else if (request.startsWith(RequestPDU.hrStorageAllocationUnits) && response.startsWith(RequestPDU.hrStorageAllocationUnits)) {
				storageData.put("hrStorageAllocationUnits", ((Integer32)variable).getValue());
			}
			else if (request.startsWith(RequestPDU.hrStorageSize) && response.startsWith(RequestPDU.hrStorageSize)) {
				storageData.put("hrStorageSize", ((Integer32)variable).getValue());
			}
			else if (request.startsWith(RequestPDU.hrStorageUsed) && response.startsWith(RequestPDU.hrStorageUsed)) {
				storageData.put("hrStorageUsed", ((Integer32)variable).getValue());
			}
			else {
				return false;
			}
		}
		else {
			return false;
		}
		
		return true;
	}
	
	private final boolean parseIPNetToMediaTable(OID response, Variable variable, OID request) {
		int [] array = response.getValue();
		int index = array[array.length -5];
		
		if (request.startsWith(RequestPDU.ipNetToMediaPhysAddress) && response.startsWith(RequestPDU.ipNetToMediaPhysAddress)) {
			OctetString value = (OctetString)variable;
			
			if (value.length() > 0) {
				this.arpTable.put(new String(value.getValue()), index);
			}
		}
		else {
			return false;
		}
		
		return true;
	}
	
	private final boolean parseCisco(OID response, Variable variable, OID request) {
		String index = Integer.toString(response.last());
		
		if (request.startsWith(RequestPDU.busyPer) && response.startsWith(RequestPDU.busyPer)) {
			this.hrProcessorEntry.put(index, (int)((Gauge32)variable).getValue());
		}
		else if (request.startsWith(RequestPDU.cpmCPUTotal5sec) && response.startsWith(RequestPDU.cpmCPUTotal5sec)) {
			this.hrProcessorEntry.put(index, (int)((Gauge32)variable).getValue());
			
		}
		else if (request.startsWith(RequestPDU.cpmCPUTotal5secRev) && response.startsWith(RequestPDU.cpmCPUTotal5secRev)) {
			this.hrProcessorEntry.put(index, (int)((Gauge32)variable).getValue());
		}
		else {
			return false;
		}
		
		return true;
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
	private final boolean parseResponse (OID response, Variable variable, OID request) throws IOException {
		if (request.startsWith(RequestPDU.system)) {
			return parseSystem(response, variable, request);
		}
		else if (request.startsWith(RequestPDU.ifEntry)) {
			return parseIFEntry(response, variable, request);
		}
		else if (request.startsWith(RequestPDU.ifXEntry)) {
			return parseIFXEntry(response, variable, request);
		}
		else if (request.startsWith(RequestPDU.host)) {
			return parseHost(response, variable, request);
		}
		else if (request.startsWith(RequestPDU.ipNetToMediaTable)) {
			return parseIPNetToMediaTable(response, variable, request);
		}
		else if (request.startsWith(RequestPDU.cisco)) {
			return parseCisco(response, variable, request);
		}
		
		return false;
	}

	public final PDU getNextRequest(PDU request, PDU response) throws IOException {
		Vector<? extends VariableBinding> requestVBs = request.getVariableBindings();
		Vector<? extends VariableBinding> responseVBs = response.getVariableBindings();
		Vector<VariableBinding> nextRequests = new Vector<VariableBinding>();
		VariableBinding requestVB, responseVB;
		Variable value;
		
		for (int i=0, length = responseVBs.size(); i<length; i++) {
			requestVB = (VariableBinding)requestVBs.get(i);
			responseVB = (VariableBinding)responseVBs.get(i);
			value = responseVB.getVariable();
			
			if (value == Null.endOfMibView) {
				continue;
			}
			
			try {
				if (parseResponse(responseVB.getOid(), value, requestVB.getOid())) {
					nextRequests.add(responseVB);
				}				
			} catch(Exception jsone) {
				jsone.printStackTrace();
			}
		}
		
		return nextRequests.size() > 0? new PDU(PDU.GETNEXT, nextRequests): null;
	}
		
	@Override
	public void onResponse(ResponseEvent event) {
		PDU request = event.getRequest();
		PDU response = event.getResponse();
		
		((Snmp)event.getSource()).cancel(request, this);

		completed = true;
		
		if (response == null) { // response timed out
			onFailure();
			
			return;
		}
		
		int status = response.getErrorStatus();
		
		if (status == PDU.noError) {
			try {
				PDU nextRequest = getNextRequest(request, response);
				
				if (nextRequest == null) {
					long current = Calendar.getInstance().getTimeInMillis();
					
					responseTime = current - this.requestTime;
					
					this.data.put("responseTime", responseTime);
					this.data.put("lastResponse", current);
					
					onSuccess();
					
					this.data.put("hrProcessorEntry", this.hrProcessorEntry);
					this.data.put("hrStorageEntry", this.hrStorageEntry);
					this.data.put("ifEntry", this.ifEntry);
					this.data.put("arpTable", this.arpTable);
				}
				else {
					sendRequest(nextRequest);
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
	
	abstract protected void onSuccess();
	abstract protected void onFailure();
	
	public static void main(String [] args) throws IOException {
		final Snmp snmp = new Snmp(new DefaultUdpTransportMapping());
		
		snmp.listen();
		
		new Node(snmp, "127.0.0.1", 161, "itahm2014", 5000) {

			@Override
			public void onSuccess() {
				System.out.println("completed!");
				
				try {
					snmp.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onFailure() {
				System.out.println("timeout.");
				
				try {
					snmp.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		}.request(RequestPDU.getInstance());
		
		System.in.read();
	}
	
}
