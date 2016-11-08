package com.itahm;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.itahm.icmp.ICMPListener;
import com.itahm.icmp.ICMPNode;
import com.itahm.table.Table;

public class ICMPAgent implements ICMPListener, Closeable {
	
	private final Map<String, ICMPNode> nodeList = new HashMap<String, ICMPNode>();
	private final Table monitorTable = ITAhM.getTable(Table.MONITOR);
	private final int timeout;
	
	public ICMPAgent(int timeout) throws IOException {
		this.timeout = timeout;
		
		JSONObject snmpData = monitorTable.getJSONObject();
		for (Object ip : snmpData.keySet()) {
			if ("icmp".equals(snmpData.getJSONObject((String)ip).getString("protocol"))) {
				addNode((String)ip);
			}
		}
		
		System.out.println("ICMP agent ready.");
	}
	
	private void addNode(String ip) {
		try {
			ICMPNode node = new ICMPNode(this, ip, timeout);
			
			this.nodeList.put(ip, node);
			
			node.start();
		} catch (UnknownHostException uhe) {
			uhe.printStackTrace();
		}		
	}
	
	public boolean removeNode(String ip) {
		ICMPNode node = this.nodeList.remove(ip);
		
		if (node == null) {
			return false;
		}
		
		node.stop();
		
		return true;
	}
	
	public ICMPNode getNode(String ip) {
		return this.nodeList.get(ip);
	}
	
	public void testNode(final String ip) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					if (InetAddress.getByName(ip).isReachable(timeout)) {
						monitorTable.getJSONObject().put(ip, new JSONObject()
							.put("protocol", "icmp")
							.put("ip", ip)
							.put("shutdown", false));
						
						monitorTable.save();
						
						addNode(ip);
						
						ITAhM.log.write(ip, String.format("%s ICMP 등록 성공.", ip), "shutdown", true);
						
						return;
					}
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				try {	
					ITAhM.log.write(ip, String.format("%s ICMP 등록 실패.", ip), "shutdown", false);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		}).start();
	}
	
	public void onSuccess(String ip) {
		ICMPNode node = this.nodeList.get(ip);
		
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
				ITAhM.log.write(ip, String.format("%s ICMP 정상.", ip), "shutdown", true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void onFailure(String ip) {
		ICMPNode node = this.nodeList.get(ip);
		
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
				
				ITAhM.log.write(ip, String.format("%s ICMP 응답 없음.", ip), "shutdown", false);
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
		for (Object ip : this.nodeList.keySet()) {
			this.nodeList.get(ip).stop();
		}
	}
	
}
