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
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

abstract public class TmpNode implements ResponseListener {

private static final long TIMEOUT = 5000;
	
	private final Snmp snmp;
	private final String ip;
	private final PDU pdu;
	private final LinkedList<CommunityTarget> list;
	private final Map<CommunityTarget, String> profileMap;
	
	public TmpNode(Snmp snmp, String ip) {
		this.snmp = snmp;
		this.ip = ip;
		
		list = new LinkedList<CommunityTarget>();
		
		profileMap = new HashMap<CommunityTarget, String>();
		
		pdu = new PDU();
		pdu.setType(PDU.GETNEXT);
		pdu.add(new VariableBinding(Constants.sysName));
	}
	
	public TmpNode addProfile(String name, int udp, String community) {
		CommunityTarget target;
		
		try {
			target = new CommunityTarget(new UdpAddress(InetAddress.getByName(this.ip), udp), new OctetString(community));
			target.setVersion(SnmpConstants.version2c);
			target.setTimeout(TIMEOUT);
			
			this.list.add(target);
			this.profileMap.put(target, name);
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		return this;
	}
	
	public void test() {
		CommunityTarget target = this.list.peek();
		
		if (target == null) {
			onTest(this.ip, null);
		}
		else {
			try {
				this.snmp.send(pdu, target, null, this);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	abstract public void onTest(String ip, String profileName);
	
	@Override
	public void onResponse(ResponseEvent event) {
		((Snmp)event.getSource()).cancel(event.getRequest(), this);

		if (event.getResponse() == null) { // response timed out
			this.list.pop();
			
			test();
		}
		else {
			onTest(this.ip, this.profileMap.get(this.list.peek()));
		}
	}
	
	public static void main(String[] args) throws IOException {
		final Snmp snmp = new Snmp(new DefaultUdpTransportMapping());
		
		snmp.listen();
		
		new TmpNode(snmp, "192.168.0.20") {
			@Override
			public void onTest(String ip, String profileName) {
				try {
					if (profileName == null) {
						System.out.println("실패");
					}
					else {
						System.out.println("성공 profile = "+ profileName);
					}
					snmp.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		.addProfile("public", 161, "public")
		.test();
		
		System.in.read();
	}
}
