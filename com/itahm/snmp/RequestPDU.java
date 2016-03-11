package com.itahm.snmp;

import org.snmp4j.PDU;
import org.snmp4j.smi.VariableBinding;

public class RequestPDU extends PDU {

	private static final long serialVersionUID = -4679454908141027650L;

	public RequestPDU() {
		setType(PDU.GETNEXT);
		add(new VariableBinding(Constants.sysDescr));
		add(new VariableBinding(Constants.sysObjectID));
		add(new VariableBinding(Constants.sysName));
		add(new VariableBinding(Constants.sysServices));
		add(new VariableBinding(Constants.ifIndex));
		add(new VariableBinding(Constants.ifDescr));
		add(new VariableBinding(Constants.ifType));
		add(new VariableBinding(Constants.ifSpeed));
		add(new VariableBinding(Constants.ifPhysAddress));
		add(new VariableBinding(Constants.ifAdminStatus));
		add(new VariableBinding(Constants.ifOperStatus));
		add(new VariableBinding(Constants.ifName));
		add(new VariableBinding(Constants.ifInOctets));
		add(new VariableBinding(Constants.ifOutOctets));
		add(new VariableBinding(Constants.ifHCInOctets));
		add(new VariableBinding(Constants.ifHCOutOctets));
		add(new VariableBinding(Constants.ifAlias));
		add(new VariableBinding(Constants.ipNetToMediaType));
		add(new VariableBinding(Constants.ipNetToMediaPhysAddress));
		add(new VariableBinding(Constants.hrSystemUptime));
		add(new VariableBinding(Constants.hrProcessorLoad));
		add(new VariableBinding(Constants.hrStorageIndex));
		add(new VariableBinding(Constants.hrStorageType));
		add(new VariableBinding(Constants.hrStorageDescr));
		add(new VariableBinding(Constants.hrStorageAllocationUnits));
		add(new VariableBinding(Constants.hrStorageSize));
		add(new VariableBinding(Constants.hrStorageUsed));
	}

}
