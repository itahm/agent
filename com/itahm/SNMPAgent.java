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
import com.itahm.table.Device;
import com.itahm.table.Profile;
import com.itahm.table.Table;
import com.itahm.util.DataCleaner;

public class SNMPAgent extends Snmp implements Closeable {
	
	private final static long REQUEST_INTERVAL = 10000;
	
	private final File snmpRoot;
	private final Map<String, SNMPNode> nodeList;
	private final Map<String, JSONObject> deviceList;
	private final Table deviceTable;
	private final Table profileTable;
	private final Table criticalTable;
	private final TopTable topTable;
	private final Timer timer;
	private final static PDU pdu = new RequestPDU();
	
	public SNMPAgent() throws IOException {
		super(new DefaultUdpTransportMapping());
		
		nodeList = new HashMap<String, SNMPNode>();
		
		deviceList = new HashMap<String, JSONObject>();
		
		deviceTable = ITAhM.getTable("device");
		
		profileTable = ITAhM.getTable("profile");
		
		criticalTable = ITAhM.getTable("critical");
		
		topTable = new TopTable();
		
		timer = new Timer();
		
		snmpRoot = new File(ITAhM.getRoot(), "snmp");
		snmpRoot.mkdir();
		
		listen();
		
		reStart();
		
		new RequestSchedule();
		
		new CleanerSchedule();
		
		System.out.println("snmp agent running...");
	}
	
	public void reStart() {
		JSONObject device;
		String ip;
		JSONObject profile;
		String profileName;
		JSONObject deviceData;
		JSONObject profileData;
		JSONObject criticalData;
		
		synchronized(this.nodeList) {
			this.nodeList.clear();
			this.deviceList.clear();
			
			deviceData = this.deviceTable.getJSONObject();
			profileData = this.profileTable.getJSONObject();
			criticalData = this.criticalTable.getJSONObject();
			
			for (Object key : deviceData.keySet()) {
				ip = (String)key;
				
				device = deviceData.getJSONObject(ip);
				
				this.deviceList.put(ip, device);
				
				if (!device.has(Device.SNMP)) {
					testNode(ip);
				}
				else if (device.getBoolean(Device.SNMP)) {
					profileName = device.getString(Device.PROFILE);
					
					if (profileData.has(profileName)) {
						profile = profileData.getJSONObject(profileName);
						
						synchronized(this.nodeList) {
							try {
								this.nodeList.put(ip
									, new SNMPNode(this, ip, profile.getInt(Constant.STRING_UDP)
									, profile.getString(Profile.COMMUNITY)
									, criticalData.has(ip)? criticalData.getJSONObject(ip): null));
							} catch (JSONException | IOException e) {
								e.printStackTrace();
							}
						}
					}
					else {
						testNode(ip);
					}
				}
			}
		}
	}
	
	public String getPeerIFName(String ip, String peerIP) {
		SNMPNode node = this.nodeList.get(ip);
		SNMPNode peerNode = this.nodeList.get(peerIP);
		
		if (node == null || peerNode == null) {
			return "";
		}
		
		return peerNode.getPeerIFName(node);
	}
	
	private String getNodeName(String ip) {
		String name = this.deviceList.get(ip).getString("name");
		
		if ("".equals(name)) {
			return ip;
		}
		else {
			return String.format("%s[%s]", ip, name);
		}
	}
	
	public void testNode(String ip) {
		final JSONObject device = deviceList.get(ip);
		final JSONObject profileData = this.profileTable.getJSONObject();
		final SNMPAgent snmp = this;
		JSONObject profile;
		
		TmpNode node = new TmpNode(this, ip) {
			@Override
			public void onTest(String profileName) {
				String sysName;
				
				if (profileName != null) {
					JSONObject profile = profileData.getJSONObject(profileName);
				
					synchronized(nodeList) {
						try {
							nodeList.put(this.ip
								, new SNMPNode(snmp, ip, profile.getInt(Constant.STRING_UDP), profile.getString(Profile.COMMUNITY), null));
						} catch (JSONException | IOException e) {
							e.printStackTrace();
							
							return;
						}
					}
					
					sysName = device.getString(Constant.STRING_NAME);
					if (sysName.length() == 0) {
						device.put(Constant.STRING_NAME, this.sysName);
					}
					
					device.put(Constant.STRING_PROFILE, profileName);
					device.put(Constant.STRING_SNMP_STATUS, true);
					device.put(Constant.STRING_SHUTDOWN, false);
					
					try {
						ITAhM.log.write(ip, Log.TEST, true, getNodeName(ip) +" 등록 성공.");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else {
					device.put(Constant.STRING_SNMP_STATUS, false);
					device.put(Constant.STRING_SHUTDOWN, true);
					
					try {
						ITAhM.log.write(ip, Log.TEST, false, getNodeName(ip) +" 등록 실패.");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				
				deviceTable.save();
			}
		};
		
		for (Object name : profileData.keySet()) {
			profile = profileData.getJSONObject((String)name);
			
			try {
				node.addProfile((String)name, profile.getInt(Constant.STRING_UDP), profile.getString(Constant.STRING_COMMUNITY));
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
	
	public void onSuccess(String ip) {
		JSONObject device = this.deviceList.get(ip);
		
		if (device.getBoolean(Constant.STRING_SHUTDOWN)) {
			device.put(Constant.STRING_SHUTDOWN, false);
			
			try {
				ITAhM.log.write(ip, Log.SHUTDOWN, false, getNodeName(ip) +" 정상.");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
	
	public void onFailure(String ip) {
		JSONObject device = this.deviceList.get(ip);

		if (!device.getBoolean(Constant.STRING_SHUTDOWN)) {
			device.put(Constant.STRING_SHUTDOWN, true);
			
			try {
				ITAhM.log.write(ip, Log.SHUTDOWN, true, getNodeName(ip) +" 응답 없음.");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void onCritical(String ip, boolean critical, String message) {
		JSONObject device = this.deviceList.get(ip);
		
		device.put(Device.CRITICAL, critical);
		
		try {
			ITAhM.log.write(ip, Log.CRITICAL, critical, getNodeName(ip) +" "+ message);
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
					
			new DataCleaner(snmpRoot, date.getTimeInMillis(), 3) {

				@Override
				public void onDelete(File file) {
				}
				
				@Override
				public void onComplete(long count) {
					System.out.println(String.format("%d 건 삭제되었습니다.", count));
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
