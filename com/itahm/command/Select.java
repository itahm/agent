package com.itahm.command;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.SNMPNode;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class Select extends Command {
	
	@Override
	protected void execute(Request request, JSONObject data) throws IOException {
		try {
			SNMPNode node = ITAhM.snmp.getNode(data.getString("ip"));
			
			if (node != null) {
				request.sendResponse(Response.getInstance(200, Response.OK, node.getData()));
				
				if (data.has("trigger") && data.getBoolean("trigger")) {
					//TODO realtime 추후 구현.
					//ITAhM.getSnmp().request(node);
				}
			}
			else {
				request.sendResponse(Response.getInstance(400, Response.BADREQUEST,
					new JSONObject().put("error", "node not found")));
			}
		}
		catch (JSONException jsone) {
			request.sendResponse(Response.getInstance(400, Response.BADREQUEST,
					new JSONObject().put("error", "invalid json request")));
		}
	}
	
}
