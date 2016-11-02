package com.itahm.snmp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Vector;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;

import com.itahm.SNMPAgent;

abstract public class TmpNode implements ResponseListener {

private static final long TIMEOUT = 5000;
	
	protected final SNMPAgent agent;
	
	private final PDU pdu;
	private final LinkedList<CommunityTarget> list;
	private final Map<CommunityTarget, String> profileMap;
	
	protected final String ip;
	
	protected String sysName;
	
	public TmpNode(SNMPAgent agent, String ip) {
		this.agent = agent;
		this.ip = ip;
		
		sysName = "";
		
		list = new LinkedList<CommunityTarget>();
		
		profileMap = new HashMap<CommunityTarget, String>();
		
		pdu = new PDU();
		pdu.setType(PDU.GETNEXT);
		pdu.add(new VariableBinding(RequestPDU.sysName));
	}
	
	public TmpNode addProfile(String name, int udp, String community) throws UnknownHostException{
		CommunityTarget target;
			
		target = new CommunityTarget(new UdpAddress(InetAddress.getByName(this.ip), udp), new OctetString(community));
		target.setVersion(SnmpConstants.version2c);
		target.setTimeout(TIMEOUT);
		
		this.list.add(target);
		this.profileMap.put(target, name);	
		
		return this;
	}
	
	public void test() {
		CommunityTarget target = this.list.peek();
		
		if (target == null) {
			onFailure();
		}
		else {
			try {
				this.agent.send(pdu, target, null, this);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void onResponse(String profileName) {
		if (profileName == null) {
			onFailure();
		}
		else {
			onSuccess(profileName);
		}
	}
	
	abstract public void onSuccess(String profileName);
	abstract public void onFailure();
	
	@Override
	public void onResponse(ResponseEvent event) {
		((Snmp)event.getSource()).cancel(event.getRequest(), this);

		if (event.getResponse() == null) { // response timed out
			this.list.pop();
			
			test();
		}
		else {
			PDU response = event.getResponse();
			int status = response.getErrorStatus();
			
			if (status == PDU.noError) {
				Vector<? extends VariableBinding> responseVBs = response.getVariableBindings();
				VariableBinding responseVB = (VariableBinding)responseVBs.get(0);
				if (responseVB.getOid().startsWith(RequestPDU.sysName)) {
					this.sysName = ((OctetString)responseVB.getVariable()).toString();
					
					onResponse(this.profileMap.get(this.list.peek()));
				}
				else {
					System.out.println("sysName 지원하지 않는 snmp.");
					
					onFailure();
				}
			}
			else {
				new Exception().printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) throws IOException {
	}
}
