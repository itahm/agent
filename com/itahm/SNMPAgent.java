package com.itahm;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
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

public class SNMPAgent extends Timer implements Closeable {
	
	private final static long REQUEST_INTERVAL = 10000;
	
	private final File snmpRoot;
	private final Snmp snmp;
	private final Map<String, SNMPNode> nodeList;
	private final Map<String, JSONObject> deviceList;
	private final Table deviceTable;
	private final Table profileTable;
	private JSONObject deviceData;
	private JSONObject profileData; 
	
	private final static PDU pdu = new RequestPDU();
	
	public SNMPAgent() throws IOException {
		snmp = new Snmp(new DefaultUdpTransportMapping());
		
		nodeList = new HashMap<String, SNMPNode>();
		
		deviceList = new HashMap<String, JSONObject>();
		
		deviceTable = ITAhM.getTable("device");
		
		profileTable = ITAhM.getTable("profile");
		
		snmpRoot = new File(ITAhM.getRoot(), "snmp");
		snmpRoot.mkdir();
		
		snmp.listen();
		
		reload();
		
		scheduleAtFixedRate(
			new TimerTask() {

				@Override
				public void run() {
					synchronized(nodeList) {
						deviceTable.save();
						
						for(String ip: nodeList.keySet()) {
							SNMPNode node = nodeList.get(ip);
							
							pdu.setRequestID(null);
							
							node.request(pdu);
						}
					}
				}
				
			}
			, 0, REQUEST_INTERVAL);
		
		System.out.println("snmp agent running...");
	}
	
	public Snmp getSNMP() {
		return this.snmp;
	}
	
	public void reload() {
		JSONObject device;
		String ip;
		JSONObject profile;
		String profileName;
		
		synchronized(this.nodeList) {
			this.nodeList.clear();
			this.deviceList.clear();
			
			this.deviceData = this.deviceTable.getJSONObject();
			this.profileData = this.profileTable.getJSONObject();
			
			for (Object key : deviceData.keySet()) {
				ip = (String)key;
				
				device = deviceData.getJSONObject(ip);
				
				this.deviceList.put(ip, device);
				
				if (!device.has(Device.SNMP)) {
					testNode(ip);
				}
				else if (device.getBoolean(Device.SNMP)) {
					profileName = device.getString(Device.PROFILE);
					
					if (this.profileData.has(profileName)) {
						profile = this.profileData.getJSONObject(profileName);
						
						addNode(ip, profile.getInt(Constant.STRING_UDP), profile.getString(Profile.COMMUNITY));
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
	
	private SNMPNode addNode(String ip, int udp, String community) {
		SNMPNode node;
		
		try {
			node = new SNMPNode(this, ip, udp, community);
		
			synchronized(this.nodeList) {
				this.nodeList.put(ip, node);
			}
			
			return node;
		} catch (JSONException | IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public void testNode(String ip) {
		final JSONObject profileData = this.profileTable.getJSONObject();
		JSONObject profile;
		
		TmpNode node = new TmpNode(this.snmp, ip) {
			@Override
			public void onTest(String ip, String profileName) {
				JSONObject device = deviceList.get(ip);
				String sysName;
				
				if (profileName != null) {
					JSONObject profile = profileData.getJSONObject(profileName);
				
					addNode(ip, profile.getInt(Constant.STRING_UDP), profile.getString(Constant.STRING_COMMUNITY));
					
					sysName = device.getString(Constant.STRING_NAME);
					if (sysName.length() == 0) {
						device.put(Constant.STRING_NAME, this.sysName);
					}
					
					device.put(Constant.STRING_PROFILE, profileName);
					device.put(Constant.STRING_SNMP_STATUS, true);
					device.put(Constant.STRING_SHUTDOWN, false);
				}
				else {
					device.put(Constant.STRING_SNMP_STATUS, false);
					device.put(Constant.STRING_SHUTDOWN, true);
				}
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
	
	public void onSuccess(String ip) {
		JSONObject device = this.deviceList.get(ip);
		
		if (device.getBoolean(Constant.STRING_SHUTDOWN)) {
			device.put(Constant.STRING_SHUTDOWN, false);
			
			String name = device.getString("name");
			String message = "down to up.";
			
			if ("".equals(name)) {
				message = String.format("%s %s", ip, message);
			}
			else {
				message = String.format("%s[%s] %s", ip, name, message);
			}
			
			try {
				ITAhM.log.write(ip, false, true, message);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
	
	public void onFailure(String ip) {
		JSONObject device = this.deviceList.get(ip);

		if (!device.getBoolean(Constant.STRING_SHUTDOWN)) {
			device.put(Constant.STRING_SHUTDOWN, true);
			
			String name = device.getString("name");
			String message = "up to down.";
			
			if ("".equals(name)) {
				message = String.format("%s %s", ip, message);
			}
			else {
				message = String.format("%s[%s] %s", ip, name, message);
			}
			
			try {
				ITAhM.log.write(ip, false, false, message);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * ovverride
	 */
	@Override
	public void close() {
		cancel();
		
		try {
			this.snmp.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
