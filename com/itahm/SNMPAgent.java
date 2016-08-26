package com.itahm;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import com.itahm.snmp.RequestPDU;
import com.itahm.snmp.TmpNode;
import com.itahm.table.Table;
import com.itahm.util.DataCleaner;

public class SNMPAgent extends Snmp implements Closeable {
	
	private final static long REQUEST_INTERVAL = 10000;
	
	public final File nodeRoot;
	
	private final Map<String, SNMPNode> nodeList;
	private final Table deviceTable;
	private final Table snmpTable;
	private final Table profileTable;
	private final Table criticalTable;
	private final TopTable topTable;
	private final Timer timer;
	private final static PDU pdu = RequestPDU.getInstance();
	
	public SNMPAgent() throws IOException {
		super(new DefaultUdpTransportMapping());
		
		System.out.println("snmp agent initialization.");
		
		nodeList = new HashMap<String, SNMPNode>();
		
		deviceTable = ITAhM.getTable(Table.DEVICE);
		
		snmpTable = ITAhM.getTable(Table.SNMP);
		
		profileTable = ITAhM.getTable(Table.PROFILE);
		
		criticalTable = ITAhM.getTable(Table.CRITICAL);
		
		topTable = new TopTable();
		
		timer = new Timer();
		
		nodeRoot = new File(ITAhM.getRoot(), "node");
		nodeRoot.mkdir();
		
		listen();
		System.out.println("snmp agent :: listening.");
		
		initNode();
		
		System.out.println("snmp agent :: loading node.");
		new RequestSchedule();
		System.out.println("snmp agent :: scheduling.");
		new CleanerSchedule();
		
		System.out.println("snmp agent :: running...");
	}
	
	public void removeNode(String ip) {
		synchronized(this.nodeList) {
			this.nodeList.remove(ip);
			
			this.snmpTable.getJSONObject().remove(ip);
			this.snmpTable.save();
		}
	}
	
	private void initNode() {
		JSONObject snmpData = this.snmpTable.getJSONObject();
		JSONObject snmp;
		JSONObject profile;
		String profileName;
		String ip;
		
		for (Object key : snmpData.keySet()) {
			ip = (String)key;
			
			snmp = snmpData.getJSONObject(ip);
			
			profileName = snmp.getString("profile");
			
			profile = this.profileTable.getJSONObject(profileName);
			
			try {
				this.nodeList.put(ip, new SNMPNode(this, ip, profile.getInt("udp")
					, profile.getString("community")
					, this.criticalTable.getJSONObject(ip)));
			} catch (JSONException | IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void resetNode(String ip, JSONObject critical) {
		SNMPNode node = this.nodeList.get(ip);
		
		node.setCritical(critical);
	}
	
	public void testNode(final String ip) {
		final JSONObject profileData = this.profileTable.getJSONObject();
		JSONObject profile;
		
		TmpNode node = new Node(this, ip);
		
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
	
	public void onSuccess(String ip) {
		JSONObject snmp = this.snmpTable.getJSONObject(ip);
		SNMPNode node = this.nodeList.get(ip);
		
		if (snmp.getBoolean("shutdown")) {
			snmp.put("shutdown", false);
			
			this.snmpTable.save();
			
			try {
				ITAhM.log.write(ip, String.format("%s [%s] 정상.", ip, node.getData().getString("sysName")));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
	
	public void onFailure(String ip) {
		JSONObject snmp = this.snmpTable.getJSONObject(ip);
		SNMPNode node = this.nodeList.get(ip);

		if (!snmp.getBoolean("shutdown")) {
			snmp.put("shutdown", true);
			
			this.snmpTable.save();
			
			try {
				ITAhM.log.write(ip, String.format("%s [%s] 응답 없음.", ip, node.getData().getString("sysName")));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void onCritical(String ip, boolean critical, String message) {
		JSONObject snmp = this.snmpTable.getJSONObject(ip);
		SNMPNode node = this.nodeList.get(ip);
		
		snmp.put("critical", critical);
		
		this.snmpTable.save();
		
		try {
			ITAhM.log.write(ip, String.format("%s [%s] %s", ip, node.getData().getString("sysName"), message));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void onSubmitTop(String ip, String resource, long value) {
		this.topTable.submit(ip, resource, value);
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
	
	class Node extends TmpNode {

		public Node(SNMPAgent agent, String ip) {
			super(agent, ip);
		}

		@Override
		public void onSuccess(String profileName) {
			JSONObject profile = profileTable.getJSONObject(profileName);
			JSONObject device = deviceTable.getJSONObject(super.ip);
			
			if ("".equals(device.getString("name"))) {
				device.put("name", super.sysName);
			}
			
			snmpTable.getJSONObject().put(super.ip, new JSONObject()
				.put("ip", super.ip)
				.put("profile", profileName)
				.put("shutdown", false)
				.put("critical", false));
			
			snmpTable.save();
			
			
			synchronized(nodeList) {
				try {
					nodeList.put(this.ip
						, new SNMPNode(super.agent, ip, profile.getInt("udp"), profile.getString("community"), null));
				} catch (JSONException | IOException e) {
					e.printStackTrace();
					
					return;
				}
			}
			
			try {
				ITAhM.log.write(ip, String.format("%s [%s] 등록 성공.", super.ip, super.sysName));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			deviceTable.save();
		}

		@Override
		public void onFailure() {
			try {
				ITAhM.log.write(ip, ip +" 등록 실패.");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	class RequestSchedule extends TimerTask {

		public RequestSchedule(){
			timer.schedule(this, 0, REQUEST_INTERVAL);
		}
		
		@Override
		public void run() {
			synchronized(nodeList) {
				for(String ip: nodeList.keySet()) {
					SNMPNode node = nodeList.get(ip);
					
					pdu.setRequestID(null);
					
					node.request(pdu);
				}
			}
		}
	}
	
	class CleanerSchedule {

		public CleanerSchedule() {
			clean();
		}
		
		public void clean() {
			Calendar date = Calendar.getInstance();
			
			date.set(Calendar.HOUR_OF_DAY, 0);
			date.set(Calendar.MINUTE, 0);
			date.set(Calendar.SECOND, 0);
			date.set(Calendar.MILLISECOND, 0);
				
			date.add(Calendar.DATE, 1);
			
			timer.schedule(new TimerTask() {
				
				@Override
				public void run() {
					clean();
				}
				
			}, date.getTime());
			
			date.add(Calendar.DATE, -1);
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

		@Override
		public int compare(String ip1, String ip2) {
			Long value1 = this.sortTop.get(ip1);
            Long value2 = this.sortTop.get(ip2);
             
            return value2.compareTo(value1);
		}
	}
}
