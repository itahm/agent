package com.itahm;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import com.itahm.snmp.TmpNode;
import com.itahm.table.Table;

public class SNMPAgent extends Snmp implements Closeable {
	
	private final static long REQUEST_INTERVAL = 10000;
	private final static OID OID_TRAP = new OID(new int [] {1,3,6,1,6,3,1,1,5});
	public final static OID	OID_LINKDOWN = new OID(new int [] {1,3,6,1,6,3,1,1,5,3});
	public final static OID	OID_LINKUP = new OID(new int [] {1,3,6,1,6,3,1,1,5,4});
	
	public final File nodeRoot;
	
	private final Map<String, SNMPNode> nodeList;
	private final Table monitorTable;
	private final Table profileTable;
	private final Table criticalTable;
	private final TopTable topTable;
	private final Timer timer;
	private final Map<String, JSONObject> arp;
	private final Map<String, String> network;
	
	private int timeout;
	
	public SNMPAgent(int timeout) throws IOException {
		super(new DefaultUdpTransportMapping(new UdpAddress("0.0.0.0/162")));
		
		System.out.println("snmp agent started.");
		
		nodeList = new ConcurrentHashMap<String, SNMPNode>();
		
		monitorTable = ITAhM.getTable(Table.MONITOR);
		
		profileTable = ITAhM.getTable(Table.PROFILE);
		
		criticalTable = ITAhM.getTable(Table.CRITICAL);
		
		topTable = new TopTable();
		
		timer = new Timer();
		
		arp = new HashMap<String, JSONObject>();
		network = new HashMap<String, String>();
		 
		nodeRoot = new File(ITAhM.getRoot(), "node");
		nodeRoot.mkdir();
		
		this.timeout = timeout;
		
		addCommandResponder(new CommandResponder() {

			@Override
			public void processPdu(CommandResponderEvent event) {
				PDU pdu = event.getPDU();
				
				if (pdu != null) {
					parseTrap(event.getPeerAddress(), event.getSecurityName(), pdu);
				}
			}
			
		});
		
		listen();
		
		initNode();
	}
	
	public void addNode(String ip, String profileName) {
		JSONObject profile = profileTable.getJSONObject(profileName);
		
		if (profile == null) {
			System.out.println(ip +" 의 프로파일 "+ profileName +" 이 존재하지 않음.");
			
			return;
		}
		
		SNMPNode node;
		
		try {
			node = new SNMPNode(this, ip, profile.getInt("udp")
					, profile.getString("community")
					, this.criticalTable.getJSONObject(ip));
			
			this.nodeList.put(ip, node);
			
			node.request(this.timeout);
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
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
			
			if ("snmp".equals(monitor.getString("protocol"))) {
				addNode(ip, monitor.getString("profile"));
			}
		}
	}
	
	public void setTimeout(int timeout) {
		this.timeout = timeout;
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
			return;
		}
		
		final JSONObject profileData = this.profileTable.getJSONObject();
		JSONObject profile;
		
		TmpNode node = new TestNode(this, ip, onFailure);
		
		for (Object name : profileData.keySet()) {
			profile = profileData.getJSONObject((String)name);
			
			try {
				node.addProfile((String)name, profile.getInt("udp"), profile.getString("community"));
			} catch (UnknownHostException | JSONException e) {
				return;
			}
		}
		
		node.test();
	}
	
	public SNMPNode getNode(String ip) {
		return this.nodeList.get(ip);
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
	
	public void onSuccess(String ip, long time) {
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
				ITAhM.log.write(ip,
					nodeData.has("sysName")? String.format("%s [%s] 정상.", ip, nodeData.getString("sysName")): String.format("%s 정상.", ip),
					"shutdown", true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
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
				
				ITAhM.log.write(ip,
					nodeData.has("sysName")? String.format("%s [%s] 응답 없음.", ip, node.getData().getString("sysName")): String.format("%s 응답 없음.", ip),
					"shutdown", false);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		node.request(this.timeout);
	}
	
	public void onResponse(String ip) {
		SNMPNode node = this.nodeList.get(ip);
		
		if (node == null) {
			return;
		}
		
		sendNextRequest(node);
	}
	
	public void onTimeout(String ip) {
		SNMPNode node = this.nodeList.get(ip);

		if (node == null) {
			return;
		}
		
		node.request(this.timeout);
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
			ITAhM.log.write(ip,
				nodeData.has("sysName")? String.format("%s [%s] %s", ip, node.getData().getString("sysName"), message): String.format("%s %s", ip, message),
				"critical", !critical);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void onSubmitTop(String ip, String resource, long value) {
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
					node.request(timeout);
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

	class TopTable implements Comparator<String> {
		private final Map<String, Long> responseTop = new HashMap<String, Long>();
		private final Map<String, Long> processorTop = new HashMap<String, Long>();
		private final Map<String, Long> memoryTop =  new HashMap<String, Long>();
		private final Map<String, Long> memoryRateTop =  new HashMap<String, Long>();
		private final Map<String, Long> storageTop = new HashMap<String, Long>();
		private final Map<String, Long> storageRateTop = new HashMap<String, Long>();
		private final Map<String, Long> throughputTop = new HashMap<String, Long>();
		private final Map<String, Long> throughputRateTop = new HashMap<String, Long>();
		private final Map<String, Long> throughputErrTop = new HashMap<String, Long>();
		private Map<String, Long> sortTop;
		
		public synchronized void submit(String ip, String resource, long value) {
			Map<String, Long> top = null;
			
			switch (resource) {
			case "responseTime":
				top = this.responseTop;
				
				break;
			case "processor":
				top = this.processorTop;
				
				break;
			case "memory":
				top = this.memoryTop;
				
				break;
			case "memoryRate":
				top = this.memoryRateTop;
				
				break;
			case "storage":
				top = this.storageTop;
				
				break;
			case "storageRate":
				top = this.storageRateTop;
				
				break;
			case "throughput":
				top = this.throughputTop;
				
				break;
			case "throughputRate":
				top = this.throughputRateTop;
				
				break;
			case "throughputErr":
				top = this.throughputErrTop;
				
				break;					
			}
			
			if (top != null) {
				top.put(ip, value);
			}
		}
		
		public synchronized JSONObject getTop(int count) {
			JSONObject top = new JSONObject();
			
			top.put("responseTime", getTop(this.responseTop, count));
			top.put("processor", getTop(this.processorTop, count));
			top.put("memory", getTop(this.memoryTop, count));
			top.put("memoryRate", getTop(this.memoryRateTop, count));
			top.put("storage", getTop(this.storageTop, count));
			top.put("storageRate", getTop(this.storageRateTop, count));
			top.put("throughput", getTop(this.throughputTop, count));
			top.put("throughputRate", getTop(this.throughputRateTop, count));
			top.put("throughputErr", getTop(this.throughputErrTop, count));
			
			return top;
		}
		
		private Map<String, Long> getTop(Map<String, Long> sortTop, int count) {
			Map<String, Long > top = new HashMap<String, Long>();
			List<String> list = new ArrayList<String>();
			String ip;
			
			this.sortTop = sortTop;
			
	        list.addAll(sortTop.keySet());
	         
	        Collections.sort(list, this);
	        
	        count = Math.min(list.size(), count);
	        for (int i=0; i< count; i++) {
	        	ip = list.get(i);
	        	
	        	top.put(ip, this.sortTop.get(ip));
	        }
	        
	        return top;
		}

		public void remove(String ip) {
			this.responseTop.remove(ip);
			this.processorTop.remove(ip);
			this.memoryTop.remove(ip);
			this.memoryRateTop.remove(ip);
			this.storageTop.remove(ip);
			this.storageRateTop.remove(ip);
			this.throughputTop.remove(ip);
			this.throughputRateTop.remove(ip);
			this.throughputErrTop.remove(ip);
		}
		
		@Override
		public int compare(String ip1, String ip2) {
			Long value1 = this.sortTop.get(ip1);
            Long value2 = this.sortTop.get(ip2);
             
            return value2.compareTo(value1);
		}
	}
	
}
