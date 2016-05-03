package com.itahm.command;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.snmp.Node;
import com.itahm.snmp.NodeList;

public class Select extends Command {
	
	@Override
	protected void execute(Request request, JSONObject data) throws IOException {
		try {
			Node node = NodeList.getNode(data.getString("ip"));
			
			if (node != null) {
				request.sendResponse(Response.getInstance(200, Response.OK, node.getData().toString()));
				
				if (data.has("trigger") && data.getBoolean("trigger")) {
					//TODO realtime 추후 구현.
					//ITAhM.getSnmp().request(node);
				}
			}
			else {
				request.sendResponse(Response.getInstance(400, Response.BADREQUEST,
					new JSONObject().put("error", "node not found").toString()));
			}
		}
		catch (JSONException jsone) {
			request.sendResponse(Response.getInstance(400, Response.BADREQUEST,
					new JSONObject().put("error", "invalid json request").toString()));
		}
	}

	
}
