package com.itahm.snmp;

import java.util.Vector;

import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

public class Responder implements CommandResponder {

	//private Service service;
	
	public Responder() {
		
	}
	
	/*
	public Responder(Service service) {
		this.service = service;
	}
	*/
	
	private final void parse(OID oid, Variable variable, Address address, String community) {
		
		if (oid.leftMostCompare(5, RequestPDU.snmpV2) == 0) {
			//if (oid.leftMostCompare(6, RequestPDU.snmpModules) == 0) {
				//if (oid.leftMostCompare(7, RequestPDU.snmpMIB) == 0) {
					//if (oid.leftMostCompare(8, RequestPDU.snmpMIBObjects) == 0) {
			if (oid.leftMostCompare(9, RequestPDU.snmpTrap) == 0) {
				System.out.println("got it");
			}
			else if (oid.leftMostCompare(9, RequestPDU.snmpTraps) == 0) {
							if (oid.leftMostCompare(10, RequestPDU.coldStart) == 0) {
								System.out.println("coldStart trap : "+ oid);
							}
							else if (oid.leftMostCompare(10, RequestPDU.warmStart) == 0) {
								System.out.println("warmStart trap : "+ oid);
							}
							else if (oid.leftMostCompare(10, RequestPDU.linkDown) == 0) {
								System.out.println("linkDown trap : "+ oid);
							}
							else if (oid.leftMostCompare(10, RequestPDU.linkUp) == 0) {
								System.out.println("linkUp trap : "+ oid);
							}
							else if (oid.leftMostCompare(10, RequestPDU.authenticationFailure) == 0) {
								System.out.println("authenticationFailure trap : "+ oid);
							}
							else if (oid.leftMostCompare(10, RequestPDU.egpNeighborLoss) == 0) {
								System.out.println("egpNeighborLoss trap : "+ oid);
							}
						}
					}
				//}
			//}
		//}
	}
	
	@Override
	public void processPdu(CommandResponderEvent event) {
		PDU response = event.getPDU();

		if (response == null) {
			// todo: time out process
			
			return;
		}
		
		Vector<? extends VariableBinding> vbs = response.getVariableBindings();
		System.out.println(vbs);
		int length = vbs.size();
		
		Address address = event.getPeerAddress();
		String community = new String(event.getSecurityName());
		
		VariableBinding vb;
		
		while (length-- > 0) {
			vb = (VariableBinding)vbs.get(length);
			
			parse(vb.getOid(), vb.getVariable(), address, community);
		}

	}

}
