package com.itahm.snmp;

import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class Request {

	private Snmp snmp;
	private CommunityTarget target;
	private PDU pdu;
	private OID oid;
	public Request(String oidStr, String type) throws IOException {
		snmp = new Snmp(new DefaultUdpTransportMapping());
		target = new CommunityTarget();
		pdu = new PDU();
		oid = new OID(oidStr);
		
		target.setVersion(SnmpConstants.version2c);
		target.setTimeout(1000);
		target.setRetries(0);
		target.setCommunity(new OctetString("public"));
		target.setAddress(new UdpAddress("127.0.0.1/161"));
		
		request(oid, type.equals("walk")? PDU.GETNEXT: PDU.GET);
	}
	
	public void request(OID oid, int type) throws IOException{
		snmp.listen();
		
		
		pdu.clear();
		pdu.setType(type);
		pdu.add(new VariableBinding(oid));
		
		ResponseEvent event = snmp.send(pdu, target);
		
		PDU response = event.getResponse();
		
		if (response == null) {
			System.out.println("time out");
		}
		else {
			if (response.getErrorStatus() != PDU.noError) {
				System.out.println(response.getErrorStatusText() +" index["+ response.getErrorIndex() +"]");
			}
			else {
				Vector<? extends VariableBinding> vbs = response.getVariableBindings();
				Iterator<? extends VariableBinding> it = vbs.iterator();
				VariableBinding vb;
				
				while (it.hasNext()) {
					vb = it.next();
					
					System.out.println("syntax : "+ vb.getSyntax());
					System.out.println("variable : "+ vb.getVariable());
					
					if (type ==  PDU.GETNEXT && vb.getOid().leftMostCompare(this.oid.size(), this.oid) == 0) {
						request(vb.getOid(), PDU.GETNEXT);
					}
					else {
						
					}
					
				}
			}
		}
		
		snmp.close();
	}
	
	public static void main(String[] args) throws Exception {
		
		if (args.length < 2) {
			throw new Exception("invalid arguments");
		}
		
		new Request(args[0], args[1]);
		
	}

}
