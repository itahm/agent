package com.itahm;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import com.itahm.enterprise.Extension;
import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.json.JSONException;
import com.itahm.json.JSONFile;
import com.itahm.json.JSONObject;

import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import com.itahm.snmp.RequestOID;
import com.itahm.snmp.TmpNode;
import com.itahm.table.Table;
import com.itahm.util.DataCleaner;
import com.itahm.util.TopTable;

public class SNMPAgent extends Snmp implements Closeable {
	
	private final static long REQUEST_INTERVAL = 10000;
	
	public enum Resource {
		RESPONSETIME("responseTime"),
		FAILURERATE("failureRate"),
		PROCESSOR("processor"),
		MEMORY("memory"),
		MEMORYRATE("memoryRate"),
		STORAGE("storage"),
		STORAGERATE("storageRate"),
		THROUGHPUT("throughput"),
		THROUGHPUTRATE("throughputRate"),
		THROUGHPUTERR("throughputErr");
		
		private String string;
		
		private Resource(String string) {
			this.string = string;
		}
		
		public String toString() {
			return this.string;
		}
	};
	
	public final File nodeRoot;
	
	private final Map<String, SNMPNode> nodeList;
	private final Table monitorTable;
	private final Table profileTable;
	private final Table criticalTable;
	private final TopTable<Resource> topTable;
	private final Timer timer;
	private final Map<String, JSONObject> arp;
	private final Map<String, String> network;
	private final Extension enterprise;
	
	public SNMPAgent(File root, boolean clean) throws IOException {
		super(new DefaultUdpTransportMapping(new UdpAddress("0.0.0.0/162")));
		
		System.out.println("SNMP manager start.");
		
		nodeList = new ConcurrentHashMap<String, SNMPNode>();
		
		monitorTable = Agent.getTable(Table.MONITOR);
		
		profileTable = Agent.getTable(Table.PROFILE);
		
		criticalTable = Agent.getTable(Table.CRITICAL);
		
		topTable = new TopTable<>(Resource.class);
		
		timer = new Timer();
		
		arp = new HashMap<String, JSONObject>();
		network = new HashMap<String, String>();
		 
		nodeRoot = new File(root, "node");
		nodeRoot.mkdir();
		
		enterprise = loadEnterprise();
		
		if (clean) {
			clean();
		}
		
		initialize();
	}
	
	public void initialize() throws IOException {
		initUSM();
		
		super.addCommandResponder(new CommandResponder() {

			@Override
			public void processPdu(CommandResponderEvent event) {
				PDU pdu = event.getPDU();
				
				if (pdu != null) {
					parseTrap(event.getPeerAddress(), event.getSecurityName(), pdu);
				}
			}
			
		});
		
		super.listen();
		
		initNode();
	}
	
	public Extension loadEnterprise() {
		try {
			URI uri = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
			
			File f = new File(new File(uri), "ITAhMEnterprise.jar");
			try (URLClassLoader ucl = new URLClassLoader(new URL [] {f.toURI().toURL()})) {
				return (Extension)(ucl.loadClass("com.itahm.enterprise.Enterprise").newInstance());
			} 
		} catch (Exception e) {
			System.out.println("enterprise is not set : "+ e.getMessage());
		}
		
		return null;
	}
	
	public void setRequestOID(PDU pdu) {
		pdu.add(new VariableBinding(RequestOID.sysDescr));
		pdu.add(new VariableBinding(RequestOID.sysObjectID));
		pdu.add(new VariableBinding(RequestOID.sysName));
		pdu.add(new VariableBinding(RequestOID.sysServices));
		pdu.add(new VariableBinding(RequestOID.ifDescr));
		pdu.add(new VariableBinding(RequestOID.ifType));
		pdu.add(new VariableBinding(RequestOID.ifSpeed));
		pdu.add(new VariableBinding(RequestOID.ifPhysAddress));
		pdu.add(new VariableBinding(RequestOID.ifAdminStatus));
		pdu.add(new VariableBinding(RequestOID.ifOperStatus));
		pdu.add(new VariableBinding(RequestOID.ifName));
		pdu.add(new VariableBinding(RequestOID.ifInOctets));
		pdu.add(new VariableBinding(RequestOID.ifInErrors));
		pdu.add(new VariableBinding(RequestOID.ifOutOctets));
		pdu.add(new VariableBinding(RequestOID.ifOutErrors));
		pdu.add(new VariableBinding(RequestOID.ifHCInOctets));
		pdu.add(new VariableBinding(RequestOID.ifHCOutOctets));
		pdu.add(new VariableBinding(RequestOID.ifHighSpeed));
		pdu.add(new VariableBinding(RequestOID.ifAlias));
		pdu.add(new VariableBinding(RequestOID.ipAdEntIfIndex));
		pdu.add(new VariableBinding(RequestOID.ipAdEntNetMask));
		pdu.add(new VariableBinding(RequestOID.ipNetToMediaType));
		pdu.add(new VariableBinding(RequestOID.ipNetToMediaPhysAddress));
		pdu.add(new VariableBinding(RequestOID.hrSystemUptime));
		pdu.add(new VariableBinding(RequestOID.hrProcessorLoad));
		pdu.add(new VariableBinding(RequestOID.hrStorageType));
		pdu.add(new VariableBinding(RequestOID.hrStorageDescr));
		pdu.add(new VariableBinding(RequestOID.hrStorageAllocationUnits));
		pdu.add(new VariableBinding(RequestOID.hrStorageSize));
		pdu.add(new VariableBinding(RequestOID.hrStorageUsed));
		
		if (this.enterprise != null) {
			this.enterprise.setRequestOID(pdu);
		}
	}
	
	public boolean parseEnterprise(SNMPNode node, OID response, Variable variable, OID request) {
		if (this.enterprise != null) {
			return this.enterprise.parseRequest(node, response, variable, request);
		}
		return false;
	}
	
	public void addNode(String ip, String profileName) {
		JSONObject profile = profileTable.getJSONObject(profileName);
		
		if (profile == null) {
			System.out.println(ip +" 의 프로파일 "+ profileName +" 이 존재하지 않음.");
			
			return;
		}
		
		SNMPNode node;
		
		try {
			if ("v3".equals(profile.getString("version"))) {
				node = SNMPNode.getInstance(this, ip, profile.getInt("udp")
						, profile.getString("user")
						, (profile.has("md5") || profile.has("sha"))? (profile.has("des")) ? SecurityLevel.AUTH_PRIV: SecurityLevel.AUTH_NOPRIV : SecurityLevel.NOAUTH_NOPRIV
						, this.criticalTable.getJSONObject(ip));
			}
			else {
				node = SNMPNode.getInstance(this, ip, profile.getInt("udp")
						, profile.getString("community")
						, this.criticalTable.getJSONObject(ip));
			}
			
			this.nodeList.put(ip, node);
			
			node.request();
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
	}
	
	public void initUSM() {
		JSONObject profileData = profileTable.getJSONObject();
		JSONObject profile;
		
		SecurityModels.getInstance().addSecurityModel(new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0));
		
		for (Object key : profileData.keySet()) {
			profile = profileData.getJSONObject((String)key);
			try {
				if ("v3".equals(profile.getString("version"))) {
					addUSM(profile);
				}
			}
			catch (JSONException | IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
	}
	
	public boolean addUSM(JSONObject profile) {
		String user = profile.getString("user");
		
		if (user.length() == 0) {
			return false;
		}
		
		String authentication = profile.has("md5")? "md5": profile.has("sha")? "sha": null;
		
		if (authentication == null) {
			return addUSM(new OctetString(user)
				, null, null, null, null);
		}
		else {
			String privacy = profile.has("des")? "des": null;
		
			if (privacy == null) {
				return addUSM(new OctetString(user)
					, "sha".equals(authentication)? AuthSHA.ID: AuthMD5.ID, new OctetString(profile.getString(authentication))
					, null, null);
			}
			
			return addUSM(new OctetString(user)
				, "sha".equals(authentication)? AuthSHA.ID: AuthMD5.ID, new OctetString(profile.getString(authentication))
				, PrivDES.ID, new OctetString(profile.getString(privacy)));
		}
	}
	
	private boolean addUSM(OctetString user, OID authProtocol, OctetString authPassphrase, OID privProtocol, OctetString privPassphrase) {		
		if (super.getUSM().getUserTable().getUser(user) != null) {
			
			return false;
		}
		
		super.getUSM().addUser(new UsmUser(user, authProtocol, authPassphrase, privProtocol, privPassphrase));
		
		return true;
	}
	
	public void removeUSM(String user) {
		super.getUSM().removeAllUsers(new OctetString(user));
	}
	
	public boolean isIdleProfile(String name) {
		JSONObject monitor;
		try {
			for (Object key : this.monitorTable.getJSONObject().keySet()) {
				monitor = this.monitorTable.getJSONObject((String)key);
				
				if (monitor.has("profile") && monitor.getString("profile").equals(name)) {
					return false;
				}
			}
		}
		catch (JSONException jsone) {
			jsone.printStackTrace();
			
			return false;
		}
		
		return true;
	}

	public boolean removeNode(String ip) {
		if (this.nodeList.remove(ip) == null) {
			return false;
		}
		
		this.topTable.remove(ip);
		
		return true;
	}
	
	private void initNode() {
		JSONObject monitorData = this.monitorTable.getJSONObject();
		JSONObject monitor;
		String ip;
		
		for (Object key : monitorData.keySet()) {
			ip = (String)key;
			
			monitor = monitorData.getJSONObject(ip);
			
			try {
				if ("snmp".equals(monitor.getString("protocol"))) {
					addNode(ip, monitor.getString("profile"));
				}
			}
			catch (JSONException jsone) {
				jsone.printStackTrace();
			}
		}
	}
	
	public void resetResponse(String ip) {
		SNMPNode node = this.nodeList.get(ip);
		
		if (node == null) {
			return;
		}
		
		node.resetResponse();
	}
	
	public void resetCritical(String ip, JSONObject critical) {
		SNMPNode node = this.nodeList.get(ip);
		
		if (node == null) {
			return;
		}
			
		node.setCritical(critical);
	}
	
	public void testNode(final String ip) {
		testNode(ip, true);
	}
	
	public void testNode(final String ip, boolean onFailure) {
		if (this.nodeList.containsKey(ip)) {
			if(onFailure) {
				try {
					Agent.manager.log.write(ip, "이미 등록된 노드 입니다.", "information", false, false);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			return;
		}
		
		final JSONObject profileData = this.profileTable.getJSONObject();
		JSONObject profile;
		
		TmpNode node = new TestNode(this, ip, onFailure);
		
		for (Object name : profileData.keySet()) {
			profile = profileData.getJSONObject((String)name);
			
			try {
				if ("v3".equals(profile.getString("version"))) {
					node.addV3Profile((String)name, profile.getInt("udp"), new OctetString(profile.getString("user"))
						, (profile.has("md5") || profile.has("sha"))? (profile.has("des")) ? SecurityLevel.AUTH_PRIV: SecurityLevel.AUTH_NOPRIV : SecurityLevel.NOAUTH_NOPRIV);
				}
				else {
					node.addProfile((String)name, profile.getInt("udp"), new OctetString(profile.getString("community")));
				}
			} catch (UnknownHostException | JSONException e) {
				e.printStackTrace();
			}
		}
		
		node.test();
	}
	
	public SNMPNode getNode(String ip) {
		return this.nodeList.get(ip);
	}
	
	public JSONObject getNodeData(String ip) {
		SNMPNode node = this.nodeList.get(ip);
		
		if (node == null) {
			return null;
		}
		
		JSONObject data = node.getData();
		
		if (data.length() > 0) {
			return data;
		}
		
		try {
			return JSONFile.getJSONObject(new File(this.nodeRoot, ip));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public JSONObject getTop(int count) {
		return this.topTable.getTop(count);		
	}
	
	public String getPeerIFName(String ip, String peerIP) {
		SNMPNode node = this.nodeList.get(ip);
		SNMPNode peerNode = this.nodeList.get(peerIP);
		
		if (node == null || peerNode == null) {
			return "";
		}
		
		return peerNode.getPeerIFName(node);
	}
	
	private void clean() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				Calendar date = Calendar.getInstance();
				
				date.set(Calendar.HOUR_OF_DAY, 0);
				date.set(Calendar.MINUTE, 0);
				date.set(Calendar.SECOND, 0);
				date.set(Calendar.MILLISECOND, 0);
				
				date.add(Calendar.MONTH, -3);
						
				new DataCleaner(nodeRoot, date.getTimeInMillis(), 3) {

					@Override
					public void onDelete(File file) {
					}
					
					@Override
					public void onComplete(long count) {
					}
				};
			}
			
		}).start();
	}
	
	public JSONObject getFailureRate(String ip) {
		SNMPNode node = this.nodeList.get(ip);
		
		if (node == null) {
			return null;
		}
		
		JSONObject json = new JSONObject();
		
		node.getFailureRate(json);
		
		return json;
	}
	
	/**
	 * ICMP 요청에 대한 응답
	 */
	public void onSuccess(String ip) {
		SNMPNode node = this.nodeList.get(ip);
		
		// 그 사이 삭제되었으면
		if (node == null) {
			return;
		}
		
		JSONObject monitor = this.monitorTable.getJSONObject(ip);
		
		if (monitor == null) {
			return;
		}
		
		if (monitor.getBoolean("shutdown")) {	
			JSONObject nodeData = node.getData();
			
			monitor.put("shutdown", false);
			
			this.monitorTable.save();
			
			try {
				Agent.manager.log.write(ip,
					nodeData.has("sysName")? String.format("%s [%s] 정상.", ip, nodeData.getString("sysName")): String.format("%s 정상.", ip),
					"shutdown", true, true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * ICMP 요청에 대한 응답
	 */
	public void onFailure(String ip) {
		SNMPNode node = this.nodeList.get(ip);

		if (node == null) {
			return;
		}
		
		JSONObject monitor = this.monitorTable.getJSONObject(ip);
		
		if (monitor == null) {
			return;
		}
		
		if (!monitor.getBoolean("shutdown")) {
			JSONObject nodeData = node.getData();
			
			monitor.put("shutdown", true);
			
			this.monitorTable.save();
			
			try {
				
				Agent.manager.log.write(ip,
					nodeData.has("sysName")? String.format("%s [%s] 응답 없음.", ip, nodeData.getString("sysName")): String.format("%s 응답 없음.", ip),
					"shutdown", false, true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * snmp 요청에 대한 응답
	 * @param ip
	 */
	public void onResponse(String ip) {
		SNMPNode node = this.nodeList.get(ip);
		
		if (node == null) {
			return;
		}
		
		try {
			File f = new File(new File(this.nodeRoot, ip), "node");
			
			JSONFile.save(f, node.getData());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		sendNextRequest(node);
	}
	
	/**
	 * snmp 요청에 대한 응답
	 * @param ip
	 */
	public void onTimeout(String ip) {
		SNMPNode node = this.nodeList.get(ip);

		if (node == null) {
			return;
		}
		
		node.request();
	}
	/**
	 * snmp 요청에 대한 응답
	 * @param ip
	 */
	public void onException(String ip) {
		SNMPNode node = this.nodeList.get(ip);

		if (node == null) {
			return;
		}
		
		sendNextRequest(node);
	}
	
	public void onCritical(String ip, boolean critical, String message) {
		SNMPNode node = this.nodeList.get(ip);
		
		if (node == null) {
			return;
		}
		
		JSONObject monitor = this.monitorTable.getJSONObject(ip);
		
		if (monitor == null) {
			return;
		}
		
		JSONObject nodeData = node.getData();
		
		monitor.put("critical", critical);
		
		this.monitorTable.save();
		
		try {
			Agent.manager.log.write(ip,
				nodeData.has("sysName")? String.format("%s [%s] %s", ip, nodeData.getString("sysName"), message): String.format("%s %s", ip, message),
				"critical", !critical, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void onSubmitTop(String ip, Resource resource, long value) {
		if (!this.nodeList.containsKey(ip)) {
			return;
		}
		
		this.topTable.submit(ip, resource, value);
	}
	
	/**
	 * 
	 * @param mac
	 * @param ip
	 * @param mask
	 */
	public void onARP(String mac, String ip, String mask) {
		if ("127.0.0.1".equals(ip)) {
			return;
		}
		
		this.arp.put(mac, new JSONObject().put("ip", ip).put("mask", mask));
	}
	
	public JSONObject getARP() {
		return new JSONObject(this.arp);
	}
	
	public void onNetwork(String ip, String mask) {
		byte [] ipArray = new IpAddress(ip).toByteArray();
		byte [] maskArray = new IpAddress(mask).toByteArray();
		int length = ipArray.length;
		
		for (int i=0; i<length; i++) {
			ipArray[i] = (byte)(ipArray[i] & maskArray[i]);
		}
		
		this.network.put(new IpAddress(ipArray).toString(), mask);
	}
	
	public JSONObject getNetwork() {
		return new JSONObject(this.network);
	}
	
	private void sendNextRequest(final SNMPNode node) {
		this.timer.schedule(
			new TimerTask() {

				@Override
				public void run() {
					node.request();
				}
				
			}, REQUEST_INTERVAL);
	}
	
	private final void parseTrap(Address addr, byte [] ba, PDU pdu) {
		String ip = ((UdpAddress)addr).getInetAddress().getHostAddress();
		SNMPNode node = this.nodeList.get(ip);
		
		if (node == null) {
			return;
		}
		
		//String community = new String(ba);
		Vector<? extends VariableBinding> vbs = pdu.getVariableBindings();
		VariableBinding vb;
		
		for (int i = 0, _i= vbs.size();i< _i; i++) {
			vb = (VariableBinding)vbs.get(i);
			
			if (this.enterprise == null || !this.enterprise.parseTrap(node, vb.getOid(), vb.getVariable())) {
				node.parseTrap(vb.getOid(), vb.getVariable());
			}
		}
	}
	
	public Response executeEnterprise(Request request, JSONObject data) {
		if (this.enterprise == null) {
			return Response.getInstance(request, Response.Status.BADREQUEST,
					new JSONObject().put("error", "enterprise is not set").toString());
		}
		
		return this.enterprise.execute(request, data);
	}
	/**
	 * ovverride
	 */
	@Override
	public void close() {
		this.timer.cancel();
		
		try {
			super.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
