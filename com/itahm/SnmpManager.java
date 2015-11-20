package com.itahm;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import org.json.JSONException;
import org.json.JSONObject;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import com.itahm.snmp.Constants;
import com.itahm.snmp.RealNode;

public class SnmpManager extends Timer implements ResponseListener, Closeable  {

	private final Map<String, RealNode> nodeMap = new HashMap<String, RealNode>();
	private final File snmpRoot;
	private final Snmp snmp = new Snmp(new DefaultUdpTransportMapping());
	private final static PDU pdu = pdu();
	
	public SnmpManager() throws IOException {
		super(true);
		
		snmpRoot = new File(ITAhM.getRoot(), "snmp");
		snmpRoot.mkdir();
		
		snmp.listen();
		
		System.out.println("snmp manager is running");
	}
	
	public void initialize() throws JSONException, IOException {
		JSONObject table = ITAhM.getTable("device").getJSONObject();
		String [] data = JSONObject.getNames(table);
		JSONObject device;
		
		for (int i=0, length=data.length; i<length; i++) {
			device = table.getJSONObject(data[i]);
			
			if (device.has("profile")) {
				addNode(device.getString("ip"), device.getString("profile"));
			}
		}
	}
	
	public void request(RealNode node) throws IOException {
		node.setRequestTime(Calendar.getInstance().getTimeInMillis());
		
		this.snmp.send(pdu, node, node, this);
	}
	
	public void request(CommunityTarget node) throws IOException {
		this.snmp.send(pdu, node, node, this);
	}
	
	public void addNode(String ip, String profile) throws IOException {
		this.nodeMap.put(ip, new RealNode(ip, profile));
	}
	
	public void removeNode(String ip) {
		RealNode node = this.nodeMap.get(ip);
		
		if (node != null) {
			node.close();
			
			nodeMap.remove(ip);
		}
	}
	
	public RealNode getNode(String ip) {
		return this.nodeMap.get(ip);
	}
	
	/**
	 * ovverride
	 */
	@Override
	public void close() throws IOException {
		cancel();
		
		this.snmp.close();
	}
	
	@Override
	public void onResponse(ResponseEvent event) {
		PDU request = event.getRequest();
		PDU response = event.getResponse();
		RealNode node = ((RealNode)event.getUserObject());
		
		((org.snmp4j.Snmp)event.getSource()).cancel(request, this);
		
		try {
			if (response == null) {			
				// TODO response timed out
				
				node.requestCompleted(false);
				return;
			}
		}
		catch (IOException ioe) {
			// TODO rolling file에 쓰기 실패하는 경우
		}
		
		int status = response.getErrorStatus();
		
		if (status == PDU.noError) {
			try {
				PDU nextRequest = node.parse(request, response);
				
				if (nextRequest == null) {
					// end of get-next request
					node.requestCompleted(true);
				}
				else {
					this.snmp.send(nextRequest, node, node, this);
				}
			} catch (IOException e) {
				// TODO fatal error
				e.printStackTrace();
			} catch (JSONException jsone) {
				jsone.printStackTrace();
			}
		}
		else {
			// TODO 
			System.out.println(String.format("error index[%d] status : %s", response.getErrorIndex(), response.getErrorStatusText()));
		}
	}
	
	public static PDU pdu() {
		PDU pdu = new PDU();
		
		pdu.setType(PDU.GETNEXT);
		pdu.add(new VariableBinding(Constants.sysDescr));
		pdu.add(new VariableBinding(Constants.sysObjectID));
		pdu.add(new VariableBinding(Constants.sysName));
		pdu.add(new VariableBinding(Constants.sysServices));
		pdu.add(new VariableBinding(Constants.ifIndex));
		pdu.add(new VariableBinding(Constants.ifDescr));
		pdu.add(new VariableBinding(Constants.ifType));
		pdu.add(new VariableBinding(Constants.ifSpeed));
		pdu.add(new VariableBinding(Constants.ifPhysAddress));
		pdu.add(new VariableBinding(Constants.ifAdminStatus));
		pdu.add(new VariableBinding(Constants.ifOperStatus));
		pdu.add(new VariableBinding(Constants.ifName));
		pdu.add(new VariableBinding(Constants.ifInOctets));
		pdu.add(new VariableBinding(Constants.ifOutOctets));
		pdu.add(new VariableBinding(Constants.ifHCInOctets));
		pdu.add(new VariableBinding(Constants.ifHCOutOctets));
		pdu.add(new VariableBinding(Constants.ifAlias));
		pdu.add(new VariableBinding(Constants.ipNetToMediaType));
		pdu.add(new VariableBinding(Constants.ipNetToMediaPhysAddress));
		pdu.add(new VariableBinding(Constants.hrSystemUptime));
		pdu.add(new VariableBinding(Constants.hrProcessorLoad));
		pdu.add(new VariableBinding(Constants.hrStorageIndex));
		pdu.add(new VariableBinding(Constants.hrStorageType));
		pdu.add(new VariableBinding(Constants.hrStorageDescr));
		pdu.add(new VariableBinding(Constants.hrStorageAllocationUnits));
		pdu.add(new VariableBinding(Constants.hrStorageSize));
		pdu.add(new VariableBinding(Constants.hrStorageUsed));
		
		return pdu;
	}
}