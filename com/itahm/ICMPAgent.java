package com.itahm;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import com.itahm.json.JSONException;
import com.itahm.json.JSONObject;

import com.itahm.icmp.ICMPListener;
import com.itahm.table.Table;

public class ICMPAgent implements ICMPListener, Closeable {
	
	private final Map<String, ICMPNode> nodeList = new HashMap<>();
	private final Table monitorTable = Agent.getTable(Table.MONITOR);
	
	public ICMPAgent() throws IOException {
		JSONObject snmpData = monitorTable.getJSONObject();
		
		for (Object ip : snmpData.keySet()) {
			try {
				if ("icmp".equals(snmpData.getJSONObject((String)ip).getString("protocol"))) {
					addNode((String)ip);
				}
			} catch (JSONException jsone) {
				jsone.printStackTrace();
			}
		}
		
		System.out.println("ICMP manager start.");
	}
	
	private void addNode(String ip) {
		try {
			ICMPNode node = new ICMPNode(this, ip);
			
			synchronized (this.nodeList) {
				this.nodeList.put(ip, node);
			}
			
			node.start();
		} catch (UnknownHostException uhe) {
			uhe.printStackTrace();
		}		
	}
	
	public boolean removeNode(String ip) {
		ICMPNode node;
		
		synchronized (this.nodeList) {
			node = this.nodeList.remove(ip);
		}
		
		if (node == null) {
			return false;
		}
		
		node.stop();
		
		return true;
	}
	
	public ICMPNode getNode(String ip) {
		synchronized(this.nodeList) {
			return this.nodeList.get(ip);
		}
	}
	
	public void testNode(final String ip) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				boolean isReachable = false;
				
				try {
					isReachable = InetAddress.getByName(ip).isReachable(Agent.DEF_TIMEOUT);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				if (!isReachable) {
					try {	
						Agent.log.write(ip, String.format("%s ICMP 등록 실패.", ip), "shutdown", false, false);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else {
					monitorTable.getJSONObject().put(ip, new JSONObject()
						.put("protocol", "icmp")
						.put("ip", ip)
						.put("shutdown", false));
					
					monitorTable.save();
					
					addNode(ip);
					
					try {
						Agent.log.write(ip, String.format("%s ICMP 등록 성공.", ip), "shutdown", true, false);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
		}).start();
	}
	
	public void onSuccess(String ip, long time) {
		ICMPNode node;
		
		synchronized (this.nodeList) {
			node = this.nodeList.get(ip);
		}
		
		if (node == null) {
			return;
		}
	
		JSONObject monitor = this.monitorTable.getJSONObject(ip);
		
		if (monitor == null) {
			return;
		}
		
		if (monitor.getBoolean("shutdown")) {
			monitor.put("shutdown", false);
			
			this.monitorTable.save();
			
			try {
				Agent.log.write(ip, String.format("%s ICMP 정상.", ip), "shutdown", true, true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void onFailure(String ip) {
		ICMPNode node;
		
		synchronized (this.nodeList) {
			node = this.nodeList.get(ip);
		}
		
		if (node == null) {
			return;
		}
		
		JSONObject monitor = this.monitorTable.getJSONObject(ip);
		
		if (monitor == null) {
			return;
		}
		
		if (!monitor.getBoolean("shutdown")) {
			monitor.put("shutdown", true);
			
			this.monitorTable.save();
			
			try {
				
				Agent.log.write(ip, String.format("%s ICMP 응답 없음.", ip), "shutdown", false, true);
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
		synchronized (this.nodeList) {
			for (ICMPNode node : this.nodeList.values()) {
				node.stop();
			}
		}
		
		this.nodeList.clear();
		
		System.out.format("ICMP manager stop.\n");
	}
	
}
