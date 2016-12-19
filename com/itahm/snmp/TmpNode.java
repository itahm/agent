package com.itahm.snmp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

abstract public class TmpNode implements ResponseListener {

private static final long TIMEOUT = 5000;
	
	//protected final SNMPAgent agent;
	protected final Snmp agent;
	
	private final PDU pdu;
	private final LinkedList<CommunityTarget> list;
	private final Map<CommunityTarget, String> profileMap;
	
	protected final String ip;
	
	public TmpNode(Snmp agent, String ip) {
		this.agent = agent;
		this.ip = ip;
		
		list = new LinkedList<>();
		
		profileMap = new HashMap<>();
		
		pdu = new PDU();
		pdu.setType(PDU.GET);
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
			int status = event.getResponse().getErrorStatus();
			
			onSuccess(this.profileMap.get(this.list.peek()));
			
			if (status != PDU.noError) {
				new Exception("status "+ status).printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) throws IOException {
		Snmp snmp = new Snmp(new DefaultUdpTransportMapping());
		
		snmp.listen();
		
		TmpNode node = new TmpNode(snmp, args[0]) {

			@Override
			public void onSuccess(String profileName) {
				System.out.println("success profile name is "+ profileName);
			}

			@Override
			public void onFailure() {
				System.out.println("falure");
			}};
			
		node.addProfile("test", Integer.parseInt(args[2]), args[1]);
		
		node.test();
		
		System.in.read();
			
		snmp.close();
	}
}
