package com.itahm.snmp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

public class NodeList {
	
	private static Map<String, Node> active = new HashMap<String, Node>();
	private static Map<String, Node> standby = new HashMap<String, Node>();
	
	public NodeList() {
	}

	public static Node join(JSONObject device) { 
		Node node = null;
		String ip = device.getString("ip");
		
		if (active.containsKey(ip)) {
			node = active.get(ip);
		}
		else {
			try {
				node = new Node(ip, device);
				
				active.put(ip, node);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if (node != null) {
			standby.put(ip, node);
		}
		
		return node;
	}
	
	public static void clear() {
		standby = new HashMap<String, Node>();
	}

	public static void reset() {
		active = standby;
	}
	
	public static Node getNode(String ip) {
		return active.get(ip);
	}
	
	public static String findInterface(String ip, String peerIP) {
		Node node = active.get(ip);
		Node peerNode = active.get(peerIP);
		
		if (node == null || peerNode == null) {
			return "";
		}
		
		return peerNode.checkInterface(node);
	}
}
