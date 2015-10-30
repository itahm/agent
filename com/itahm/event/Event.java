package com.itahm.event;

import java.util.Calendar;

import org.json.JSONObject;

public class Event extends JSONObject {
	
	private String index;
	
	public Event(String sysName, String ipAddr, String resource, long lastStatus, long currentStatus, String text) {
		put("timeStamp", Calendar.getInstance().getTimeInMillis());
		put("sysName", sysName);
		put("ipAddr", ipAddr);
		put("resource", resource);
		put("lastStatus", lastStatus);
		put("currentStatus", currentStatus);
		put("text", text);
	}
	
	public Event(String sysName, String ipAddr, String resource, String index, long lastStatus, long currentStatus, String text) {
		this(sysName, ipAddr, resource, lastStatus, currentStatus, text);
		
		put("index", Integer.parseInt(index));
	}
	
	public void index(int index) {
		this.index = Integer.toString(index);
	}
	
	public String toString() {
		JSONObject result = new JSONObject();
		
		result.put("command", "event");
		result.put("data", this.index == null? JSONObject.NULL: new JSONObject().put(index, this));
		
		return result.toString();
	}

}
