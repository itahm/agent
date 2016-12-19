package com.itahm.command;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.table.Table;

public class Link implements Command {
	
	@Override
	public Response execute(Request request, JSONObject data) throws IOException {
		Table table = ITAhM.getTable("device");
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
					ifEntry1.put(ip2, ITAhM.agent.snmp.getPeerIFName(ip1, ip2));
					ifEntry2.put(ip1, ITAhM.agent.snmp.getPeerIFName(ip2, ip1));
				}
				catch(NullPointerException npe) {
					return Response.getInstance(Response.Status.UNAVAILABLE);
				}
			}
			else {
				ifEntry1.remove(ip2);
				ifEntry2.remove(ip1);
			}
			
			return Response.getInstance(Response.Status.OK, table.save().toString());
		}
		catch (JSONException jsone) {jsone.printStackTrace();
			return Response.getInstance(Response.Status.BADREQUEST,
				new JSONObject().put("error", "invalid json request").toString());
		}
	}

}
