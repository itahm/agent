package com.itahm;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import com.itahm.json.JSONException;
import com.itahm.json.JSONFile;
import com.itahm.json.JSONObject;

import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.AuthMD5;
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

import com.itahm.snmp.TmpNode;
import com.itahm.table.Table;
import com.itahm.util.DataCleaner;
import com.itahm.util.TopTable;

public class SNMPAgent extends Snmp implements Closeable {
	
	private final static long REQUEST_INTERVAL = 10000;
	private final static OID OID_TRAP = new OID(new int [] {1,3,6,1,6,3,1,1,5});
	public final static OID	OID_LINKDOWN = new OID(new int [] {1,3,6,1,6,3,1,1,5,3});
	public final static OID	OID_LINKUP = new OID(new int [] {1,3,6,1,6,3,1,1,5,4});
	
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
	public void initUSM() {
		JSONObject profileData = profileTable.getJSONObject();
		USM usm;
		JSONObject profile;
		OctetString user;
		OctetString auth;
		OctetString priv;
		
		SecurityModels.getInstance().addSecurityModel(new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0));
		
		usm = super.getUSM();
		
		for (Object key : profileData.keySet()) {
			profile = profileData.getJSONObject((String)key);
			try {
				if ("v3".equals(profile.getString("version"))) {
					user = new OctetString(profile.getString("user"));
					
					if (user.length() == 0) {
						throw new IllegalArgumentException();
					}
					
					auth = profile.has("authentication")? new OctetString(profile.getString("authentication")): null;
					priv = profile.has("privacy")? new OctetString(profile.getString("privacy")): null;
					
					usm.addUser(user, new UsmUser(user, auth == null? null: AuthMD5.ID, auth, priv == null? null: PrivDES.ID, priv));
				}
			}
			catch (JSONException | IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
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
				int level = SecurityLevel.NOAUTH_NOPRIV;
				
				if (profile.has("authentication")) {
					level = SecurityLevel.AUTH_NOPRIV;
					
					if (profile.has("privacy")) {
						level = SecurityLevel.AUTH_PRIV;
					}
				}
				
				node = SNMPNode.getInstance(this, ip, profile.getInt("udp")
						, profile.getString("user")
						, level
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
	
	private boolean addUSM(OctetString user, OID authProtocol, OctetString authPassphrase, OID privProtocol, OctetString privPassphrase) {		
		if (super.getUSM().getUser(null, user) != null) {
			return false;
		}
		
		super.getUSM().addUser(new UsmUser(user, authProtocol, authPassphrase, privProtocol, privPassphrase));
		
		return true;
	}
	
	public boolean addUSM(String user) {
		return addUSM(new OctetString(user), null, null, null, null);
	}
	
	public boolean addUSM(String user, String auth) {
		return addUSM(new OctetString(user), AuthMD5.ID, new OctetString(auth), null, null);
	}
	
	public boolean addUSM(String user, String auth, String priv) {
		return addUSM(new OctetString(user), AuthMD5.ID, new OctetString(auth), null, null);
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
		int level;
		
		for (Object name : profileData.keySet()) {
			profile = profileData.getJSONObject((String)name);
			
			try {
				if ("v3".equals(profile.getString("version"))) {
					level = SecurityLevel.NOAUTH_NOPRIV;
					
					if (profile.has("authentication")) {
						level = SecurityLevel.AUTH_NOPRIV;
						
						if (profile.has("privacy")) {
							level = SecurityLevel.AUTH_PRIV;
						}
					}
					
					node.addV3Profile((String)name, profile.getInt("udp"), new OctetString(profile.getString("user")), level);
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
		Calendar date = Calendar.getInstance();
		
		date.set(Calendar.HOUR_OF_DAY, 0);
		date.set(Calendar.MINUTE, 0);
		date.set(Calendar.SECOND, 0);
		date.set(Calendar.MILLISECOND, 0);
		
		date.add(Calendar.MONTH, -3);
				
		new DataCleaner(this.nodeRoot, date.getTimeInMillis(), 3) {

			@Override
			public void onDelete(File file) {
			}
			
			@Override
			public void onComplete(long count) {
			}
		};
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
		
		if (!this.nodeList.containsKey(ip)) {
			return;
		}
		
		//String community = new String(ba);
		Vector<? extends VariableBinding> vbs = pdu.getVariableBindings();
		VariableBinding vb;
		
		for (int i = 0, _i= vbs.size();i< _i; i++) {
			vb = (VariableBinding)vbs.get(i);
			
			parseTrap(vb.getOid(), vb.getVariable());
		}
	}
	
	private void parseTrap(OID oid, Variable variable) {
		if (OID_TRAP.leftMostCompare(OID_TRAP.size(), oid) != 0) {
			return;
		}
		
		if (OID_LINKDOWN.leftMostCompare(OID_LINKDOWN.size(), oid) == 0) {
			System.out.println("linkDown trap : "+ oid);
		}
		else if (OID_LINKUP.leftMostCompare(OID_LINKUP.size(), oid) == 0) {
			System.out.println("linkUp trap : "+ oid);
		}
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
