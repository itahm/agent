package com.itahm.command;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.http.Session;
import com.itahm.table.Table;

public class Link extends Command {
	
	public void execute(Request request, JSONObject data, Session session) throws IOException {
		execute(request, data);
	}
		
	@Override
	protected void execute(Request request, JSONObject data) throws IOException {
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
				ifEntry1.put(ip2, ITAhM.agent.getPeerIFName(ip1, ip2));
				ifEntry2.put(ip1, ITAhM.agent.getPeerIFName(ip2, ip1));
			}
			else {
				ifEntry1.remove(ip2);
				ifEntry2.remove(ip1);
			}
			
			request.sendResponse(Response.getInstance(200, Response.OK, table.save()));
		}
		catch (JSONException jsone) {jsone.printStackTrace();
			request.sendResponse(Response.getInstance(400, Response.BADREQUEST,
					new JSONObject().put("error", "invalid json request")));
		}
	}

}
