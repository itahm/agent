package com.itahm.snmp;

import org.snmp4j.smi.OID;

public class Constants {
	public final static OID iso = new OID(new int [] {1});
	public final static OID org = new OID(new int [] {1,3}); // iso.org
	public final static OID dod = new OID(new int [] {1,3,6}); // iso.org.dod
	public final static OID internet = new OID(new int [] {1,3,6,1}); // iso.org.dod.internet
	public final static OID mgmt = new OID(new int [] {1,3,6,1,2}); // iso.org.dod.internet.mgmt
	/*
	 * RFC1213-MIB
	 */
	// iso(1).org(3).dod(6).internet(1).mgmt(2).mib_2(1)
	public final static OID mib_2 = new OID(new int [] {1,3,6,1,2,1});
	// iso.org.dod.internet.mgmt.mib_2.system
	public final static OID 	system = new OID(new int [] {1,3,6,1,2,1,1});
	// iso.org.dod.internet.mgmt.mib_2.system.sysDescr
	public final static OID 	sysDescr = new OID(new int [] {1,3,6,1,2,1,1,1});
	// iso.org.dod.internet.mgmt.mib_2.system.sysObjectID
	public final static OID 	sysObjectID = new OID(new int [] {1,3,6,1,2,1,1,2});
	// iso.org.dod.internet.mgmt.mib_2.system.sysServices
	public final static OID 	sysServices = new OID(new int [] {1,3,6,1,2,1,1,7});
	// iso.org.dod.internet.mgmt.mib_2.system.sysName
	public final static OID sysName =  new OID(new int [] {1,3,6,1,2,1,1,5});
	// iso(1).org(3).dod(6).internet(1).mgmt(2).mib_2(1).interfaces(2)
	public final static OID interfaces = new OID(new int [] {1,3,6,1,2,1,2});
	// iso.org.dod.internet.mgmt.mib_2.interfaces.ifNumber
	//public final static OID ifNumber =  new OID(new int [] {1,3,6,1,2,1,2,1});
	// iso.org.dod.internet.mgmt.mib_2.interfaces.ifTable.ifEntry
	public final static OID ifEntry =  new OID(new int [] {1,3,6,1,2,1,2,2,1});
	// iso.org.dod.internet.mgmt.mib_2.interfaces.ifTable.ifEntry.ifIndex
	public final static OID ifIndex =  new OID(new int [] {1,3,6,1,2,1,2,2,1,1});
	// iso.org.dod.internet.mgmt.mib_2.interfaces.ifTable.ifEntry.ifIDescr
	public final static OID ifDescr = new OID(new int [] {1,3,6,1,2,1,2,2,1,2});
	// iso.org.dod.internet.mgmt.mib_2.interfaces.ifTable.ifEntry.ifType
	public final static OID ifType =  new OID(new int [] {1,3,6,1,2,1,2,2,1,3});
	// iso.org.dod.internet.mgmt.mib_2.interfaces.ifTable.ifEntry.ifSpeed
	public final static OID ifSpeed =  new OID(new int [] {1,3,6,1,2,1,2,2,1,5});
	//iso.org.dod.internet.mgmt.mib_2.interfaces.ifTable.ifEntry.ifPhysAddress
	public final static OID ifPhysAddress =  new OID(new int [] {1,3,6,1,2,1,2,2,1,6});
	//iso.org.dod.internet.mgmt.mib_2.interfaces.ifTable.ifEntry.ifAdminStatus
	public final static OID ifAdminStatus =  new OID(new int [] {1,3,6,1,2,1,2,2,1,7});
	//iso.org.dod.internet.mgmt.mib_2.interfaces.ifTable.ifEntry.ifOperStatus
	public final static OID ifOperStatus =  new OID(new int [] {1,3,6,1,2,1,2,2,1,8});
	// iso(1).org(3).dod(6).internet(1).mgmt(2).mib_2(1).interfaces(2).ifTable(2).ifEntry(1).ifInOctets(10)
	public final static OID ifInOctets =  new OID(new int [] {1,3,6,1,2,1,2,2,1,10});
	//iso.org.dod.internet.mgmt.mib_2.interfaces.ifTable.ifEntry.ifInUcastPkts
	public final static OID ifInUcastPkts =  new OID(new int [] {1,3,6,1,2,1,2,2,1,11});
	//iso.org.dod.internet.mgmt.mib_2.interfaces.ifTable.ifEntry.ifInNUcastPkts
	public final static OID ifInNUcastPkts =  new OID(new int [] {1,3,6,1,2,1,2,2,1,12});
	//iso.org.dod.internet.mgmt.mib_2.interfaces.ifTable.ifEntry.ifOutOctets
	public final static OID ifOutOctets =  new OID(new int [] {1,3,6,1,2,1,2,2,1,16});
	//iso.org.dod.internet.mgmt.mib_2.interfaces.ifTable.ifEntry.ifOutUcastPkts
	public final static OID ifOutUcastPkts =  new OID(new int [] {1,3,6,1,2,1,2,2,1,17});
	//iso.org.dod.internet.mgmt.mib_2.interfaces.ifTable.ifEntry.ifOutNUcastPkts
	public final static OID ifOutNUcastPkts =  new OID(new int [] {1,3,6,1,2,1,2,2,1,18});
	public final static OID 			at = new OID(mib_2).append(3);  	// 7
	public final static OID 				atTable = new OID(at).append(1); // 8
	public final static OID 					atEntry = new OID(atTable).append(1); // 9
	public final static OID 						atPhysAddress = new OID(atEntry).append(2); // 10
	public final static OID 						atNetAddress = new OID(atEntry).append(3); // 10
	public final static OID 			ip = new OID(mib_2).append(4); // 7
	//iso(1).org(3).dod(6).internet(1).mgmt(2).mib_2(1).at(3).atTable(1).atEntry(1)
	
	//iso(1).org(3).dod(6).internet(1).mgmt(2).mib_2(1).ip(4).ipNetToMediaTable(22)
	public final static OID ipNetToMediaTable = new OID(new int [] {1,3,6,1,2,1,4,22});
	public final static OID ipNetToMediaPhysAddress = new OID(new int [] {1,3,6,1,2,1,4,22,1,2});
	public final static OID ipNetToMediaType = new OID(new int [] {1,3,6,1,2,1,4,22,1,4});
	
	/*
	 * HOST-RESOURCES-MIB
	 */
	//iso(1).org(3).dod(6).internet(1).mgmt(2).mib_2(1).host(25)
	public final static OID host = new OID(new int [] {1,3,6,1,2,1,25});
	public final static OID hrSystemUptime = new OID(new int [] {1,3,6,1,2,1,25,1,1});
	
	public final static OID hrStorageTypes = new OID(new int [] {1,3,6,1,2,1,25,2,1});
	public final static OID hrStorageEntry = new OID(new int [] {1,3,6,1,2,1,25,2,3,1});
	public final static OID hrStorageIndex = new OID(new int [] {1,3,6,1,2,1,25,2,3,1,1});
	public final static OID hrStorageType = new OID(new int [] {1,3,6,1,2,1,25,2,3,1,2});
	public final static OID hrStorageDescr = new OID(new int [] {1,3,6,1,2,1,25,2,3,1,3});
	public final static OID hrStorageAllocationUnits = new OID(new int [] {1,3,6,1,2,1,25,2,3,1,4});
	public final static OID hrStorageSize = new OID(new int [] {1,3,6,1,2,1,25,2,3,1,5});
	public final static OID hrStorageUsed = new OID(new int [] {1,3,6,1,2,1,25,2,3,1,6});
	
	public final static OID hrProcessorLoad = new OID(new int [] {1,3,6,1,2,1,25,3,3,1,2});
	
	//iso(1).org(3).dod(6).internet(1).mgmt(2).mib_2(1).ifMib(31)
	public final static OID ifMib = new OID(new int [] {1,3,6,1,2,1,31});
	//iso(1).org(3).dod(6).internet(1).mgmt(2).mib_2(1).ifMib(31).ifMibObjects(1)
	public final static OID ifMibObjects = new OID(new int [] {1,3,6,1,2,1,31,1});
	//iso(1).org(3).dod(6).internet(1).mgmt(2).mib_2(1).ifMib(31).ifMibObjects(1).ifXTable(1)
	public final static OID ifXTable = new OID(new int [] {1,3,6,1,2,1,31,1,1});
	//iso(1).org(3).dod(6).internet(1).mgmt(2).mib_2(1).ifMib(31).ifMibObjects(1).ifXTable(1).ifXEntry(1)
	public final static OID ifXEntry = new OID(new int [] {1,3,6,1,2,1,31,1,1,1});
	public final static OID 							ifName =  new OID(ifXEntry).append(1); // 11
	public final static OID 							ifHCInOctets =  new OID(ifXEntry).append(6); // 11
	public final static OID 							ifHCInUcastPkts =  new OID(ifXEntry).append(7); // 11
	public final static OID 							ifHCInMulticastPkts =  new OID(ifXEntry).append(8); // 11
	public final static OID 							ifHCInBroadcastPkts =  new OID(ifXEntry).append(9); // 11
	//iso(1).org(3).dod(6).internet(1).mgmt(2).mib_2(1).ifMib(31).ifMibObjects(1).ifXTable(1).ifXEntry(1).ifHCOutOctets(10)
	public final static OID ifHCOutOctets =  new OID(new int [] {1,3,6,1,2,1,31,1,1,1,10});
	public final static OID 							ifHCOutUcastPkts =  new OID(ifXEntry).append(11); // 11
	public final static OID 							ifHCOutMulticastPkts =  new OID(ifXEntry).append(12); // 11
	public final static OID 							ifHCOutBroadcastPkts =  new OID(ifXEntry).append(13); // 11
	//iso(1).org(3).dod(6).internet(1).mgmt(2).mib_2(1).ifMib(31).ifMibObjects(1).ifXTable(1).ifXEntry(1).ifAlias(18)
	public final static OID ifAlias = new OID(new int [] {1,3,6,1,2,1,31,1,1,1,18});
	
	//public final static OID 	private = new OID(internet).append(4); // 5
	//public final static OID 		enterprises = new OID(internet).append(4).append(1); // 6

	// iso.org.dod.internet.snmpV2
	public final static OID snmpV2 = new OID(new int [] {1,3,6,1,6});
	// iso.org.dod.internet.snmpV2.snmpModules
	public final static OID snmpModules = new OID(new int [] {1,3,6,1,6,3});
	// iso.org.dod.internet.snmpV2.snmpModules.snmpMIB
	public final static OID snmpMIB = new OID(new int [] {1,3,6,1,6,3,1});
	// iso.org.dod.internet.snmpV2.snmpModules.snmpMIB.snmpMIBObjects
	public final static OID snmpMIBObjects = new OID(new int [] {1,3,6,1,6,3,1,1});
	// internet.snmpV2.snmpModules.snmpMIB.snmpMIBObjects.snmpTrap
	public final static OID	snmpTrap = new OID(new int [] {1,3,6,1,6,3,1,1,4});
	// internet.snmpV2.snmpModules.snmpMIB.snmpMIBObjects.snmpTrap.snmpTrapOID
	public final static OID	snmpTrapOID = new OID(new int [] {1,3,6,1,6,3,1,1,4,1});
	// internet.snmpV2.snmpModules.snmpMIB.snmpMIBObjects.snmpTraps
	public final static OID	snmpTraps = new OID(new int [] {1,3,6,1,6,3,1,1,5});
	// internet.snmpV2.snmpModules.snmpMIB.snmpMIBObjects.snmpTraps.coldStart
	public final static OID	coldStart = new OID(new int [] {1,3,6,1,6,3,1,1,5,1});
	// internet.snmpV2.snmpModules.snmpMIB.snmpMIBObjects.snmpTraps.warmStart
	public final static OID	warmStart = new OID(new int [] {1,3,6,1,6,3,1,1,5,2});
	// internet.snmpV2.snmpModules.snmpMIB.snmpMIBObjects.snmpTraps.linkDown
	public final static OID	linkDown = new OID(new int [] {1,3,6,1,6,3,1,1,5,3});
	// internet.snmpV2.snmpModules.snmpMIB.snmpMIBObjects.snmpTraps.linkUp
	public final static OID	linkUp = new OID(new int [] {1,3,6,1,6,3,1,1,5,4});
	
	public final static OID 						authenticationFailure = new OID(snmpTraps).append(5); // 10
	public final static OID 						egpNeighborLoss = new OID(snmpTraps).append(6); // 10
	
	/**
	 * "hrProcessorLoad"
	 */
	public static final String HRPROCESSORLOAD = "hrProcessorLoad";
	
	/**
	 * "ifInOctets"
	 */
	public static final String IFINOCTETS = "ifInOctets";
	
	/**
	 * "ifOutOctets"
	 */
	public static final String IFOUTOCTETS = "ifOutOctets";
	
	/**
	 * "hrStorageUsed"
	 */
	public static final String HRSTORAGEUSED = "hrStorageUsed";
	
	public Constants() {
		// TODO Auto-generated constructor stub
	}

}