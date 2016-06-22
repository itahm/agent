package com.itahm.snmp;

import java.io.File;
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
	
	protected final JSONObject data;
	
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
		
		// file 및 json 초기화
		File nodeRoot = new File(new File(ITAhM.getRoot(), Constant.STRING_SNMP), ip);
		nodeRoot.mkdirs();
	}
	
	public void request (PDU pdu) {
		if (!completed) {
			ITAhM.debug("delay");
			
			return;
		}
		
		completed = false;
		
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
		if (request.startsWith(Constants.sysDescr) && response.startsWith(Constants.sysDescr)) {
			this.data.put("sysDescr", new String(((OctetString)variable).getValue()));
		}
		else if (request.startsWith(Constants.sysObjectID) && response.startsWith(Constants.sysObjectID)) {
			this.data.put("sysObjectID", ((OID)variable).toDottedString());
		}
		else if (request.startsWith(Constants.sysName) && response.startsWith(Constants.sysName)) {
			this.data.put("sysName", new String(((OctetString)variable).getValue()));
		}
		
		return false;
	}
	
	private final boolean parseIFEntry(OID response, Variable variable, OID request) throws IOException {
		String index = Integer.toString(response.last());
		JSONObject ifData = this.ifEntry.get(index);
		
		if(ifData == null) {
			this.ifEntry.put(index, ifData = new JSONObject());
			
			ifData.put("ifInBPS", 0);
			ifData.put("ifOutBPS", 0);
		}
		
		if (request.startsWith(Constants.ifDescr) && response.startsWith(Constants.ifDescr)) {
			ifData.put("ifDescr", new String(((OctetString)variable).getValue()));
		}
		else if (request.startsWith(Constants.ifType) && response.startsWith(Constants.ifType)) {			
			ifData.put("ifType", ((Integer32)variable).getValue());
		}
		else if (request.startsWith(Constants.ifSpeed) && response.startsWith(Constants.ifSpeed)) {			
			ifData.put(Constant.STRING_IFSPEED, ((Gauge32)variable).getValue());
		}
		else if (request.startsWith(Constants.ifPhysAddress) && response.startsWith(Constants.ifPhysAddress)) {
			ifData.put(Constant.STRING_MAC_ADDR, new String(((OctetString)variable).getValue()));
		}
		else if (request.startsWith(Constants.ifAdminStatus) && response.startsWith(Constants.ifAdminStatus)) {
			ifData.put(Constant.STRING_IFADMINSTAT, ((Integer32)variable).getValue());
		}
		else if (request.startsWith(Constants.ifOperStatus) && response.startsWith(Constants.ifOperStatus)) {			
			ifData.put("ifOperStatus", ((Integer32)variable).getValue());
		}
		else if (request.startsWith(Constants.ifInOctets) && response.startsWith(Constants.ifInOctets)) {
			ifData.put("ifInOctets", ((Counter32)variable).getValue());
		}
		else if (request.startsWith(Constants.ifOutOctets) && response.startsWith(Constants.ifOutOctets)) {
			ifData.put("ifOutOctets", ((Counter32)variable).getValue());
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
			this.ifEntry.put(index, ifData = new JSONObject());
		}
		
		if (request.startsWith(Constants.ifName) && response.startsWith(Constants.ifName)) {
			ifData.put(Constant.STRING_IFNAME, new String(((OctetString)variable).getValue()));
		}
		else if (request.startsWith(Constants.ifAlias) && response.startsWith(Constants.ifAlias)) {
			ifData.put(Constant.STRING_IFALIAS, new String(((OctetString)variable).getValue()));
		}
		else if (request.startsWith(Constants.ifHCInOctets) && response.startsWith(Constants.ifHCInOctets)) {
			ifData.put(Constant.STRING_IFHCIN, ((Counter64)variable).getValue());
		}
		else if (request.startsWith(Constants.ifHCOutOctets) && response.startsWith(Constants.ifHCOutOctets)) {
			ifData.put(Constant.STRING_IFHCOUT, ((Counter64)variable).getValue());
		}
		else {
			return false;
		}
		
		return true;
	}
	
	private final boolean parseHost(OID response, Variable variable, OID request) throws JSONException, IOException {
		if (request.startsWith(Constants.hrSystemUptime) && response.startsWith(Constants.hrSystemUptime)) {
			this.data.put(Constant.STRING_SYSUPTIME, ((TimeTicks)variable).toMilliseconds());
			
			return false;
		}
		
		String index = Integer.toString(response.last());
		
		if (request.startsWith(Constants.hrProcessorLoad) && response.startsWith(Constants.hrProcessorLoad)) {
			this.hrProcessorEntry.put(index, ((Integer32)variable).getValue());
		}
		else if (request.startsWith(Constants.hrStorageEntry) && response.startsWith(Constants.hrStorageEntry)) {
			JSONObject storageData = this.hrStorageEntry.get(index);
			
			if (storageData == null) {
				storageData = new JSONObject();
				
				this.hrStorageEntry.put(index, storageData = new JSONObject());
			}
			
			if (request.startsWith(Constants.hrStorageType) && response.startsWith(Constants.hrStorageType)) {
				storageData.put("hrStorageType", ((OID)variable).last());
			}
			else if (request.startsWith(Constants.hrStorageDescr) && response.startsWith(Constants.hrStorageDescr)) {
				storageData.put("hrStorageDescr", new String(((OctetString)variable).getValue()));
			}
			else if (request.startsWith(Constants.hrStorageAllocationUnits) && response.startsWith(Constants.hrStorageAllocationUnits)) {
				storageData.put("hrStorageAllocationUnits", ((Integer32)variable).getValue());
			}
			else if (request.startsWith(Constants.hrStorageSize) && response.startsWith(Constants.hrStorageSize)) {
				storageData.put("hrStorageSize", ((Integer32)variable).getValue());
			}
			else if (request.startsWith(Constants.hrStorageUsed) && response.startsWith(Constants.hrStorageUsed)) {
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
		
		if (request.startsWith(Constants.ipNetToMediaPhysAddress) && response.startsWith(Constants.ipNetToMediaPhysAddress)) {
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
		if (request.startsWith(Constants.system)) {
			return parseSystem(response, variable, request);
		}
		else if (request.startsWith(Constants.ifEntry)) {
			return parseIFEntry(response, variable, request);
		}
		else if (request.startsWith(Constants.ifXEntry)) {
			return parseIFXEntry(response, variable, request);
		}
		else if (request.startsWith(Constants.host)) {
			return parseHost(response, variable, request);
		}
		else if (request.startsWith(Constants.ipNetToMediaTable)) {
			return parseIPNetToMediaTable(response, variable, request);
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
			
		}.request(new RequestPDU());
		
		System.in.read();
	}
	
}
