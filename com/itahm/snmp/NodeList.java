package com.itahm.snmp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

public class NodeList {

	private Map<String, Node> active = new HashMap<String, Node>();
	private Map<String, Node> standby = new HashMap<String, Node>();
	private Map<Node, JSONObject> deviceList = new HashMap<Node, JSONObject>();
	
	public NodeList() {
	}

	public Node join(JSONObject device) { 
		Node node = null;
		String ip = device.getString("ip");
		
		if (this.active.containsKey(ip)) {
			node = this.active.get(ip);
		}
		else {
			try {
				node = new Node(ip);
				
				this.active.put(ip, node);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if (node != null) {
			standby.put(ip, node);
			
			this.deviceList.put(node, device);
		}
		
		return node;
	}
	
	public void clear() {
		this.standby = new HashMap<String, Node>();
	}

	public void reset() {
		this.active = this.standby;
	}
	
	public Node getNode(String ip) {
		return this.active.get(ip);
	}
	
	public JSONObject getDevice(Node node) {
		return this.deviceList.get(node);
	}
	
}
