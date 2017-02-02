package com.itahm.command;

import java.io.IOException;

import com.itahm.json.JSONException;
import com.itahm.json.JSONObject;

import com.itahm.Agent;
import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.table.Table;

public class Link implements Command {
	
	@Override
	public Response execute(Request request, JSONObject data) throws IOException {
		Table table = Agent.getTable("device");
		JSONObject deviceData = table.getJSONObject();
		
		try {
			String ip1 = data.getString("peer1");
			String ip2 = data.getString("peer2");
			boolean link = data.getBoolean("link");
			JSONObject device1 = deviceData.getJSONObject(ip1);
			JSONObject device2 = deviceData.getJSONObject(ip2);
			JSONObject ifEntry1 = device1.getJSONObject("ifEntry");
			JSONObject ifEntry2 = device2.getJSONObject("ifEntry");
			
			if (link) {
				try {
					ifEntry1.put(ip2, Agent.manager.snmp.getPeerIFName(ip1, ip2));
					ifEntry2.put(ip1, Agent.manager.snmp.getPeerIFName(ip2, ip1));
				}
				catch(NullPointerException npe) {
					return Response.getInstance(request, Response.Status.UNAVAILABLE);
				}
			}
			else {
				ifEntry1.remove(ip2);
				ifEntry2.remove(ip1);
			}
			
			return Response.getInstance(request, Response.Status.OK, table.save().toString());
		}
		catch (JSONException jsone) {
			return Response.getInstance(request, Response.Status.BADREQUEST,
				new JSONObject().put("error", "invalid json request").toString());
		}
	}

}
