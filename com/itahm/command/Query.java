package com.itahm.command;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.snmp.Node;
import com.itahm.snmp.NodeList;

public class Query extends Command {
	
	@Override
	protected void execute(Request request, JSONObject data) throws IOException {
		
		try {
			Node node = NodeList.getNode(data.getString("ip"));
			
			if (node != null) {
				data = node.getData(data.getString("database")
					, String.format("%d", data.getInt("index"))
					, data.getLong("start")
					, data.getLong("end")
					, data.has("summary")? data.getBoolean("summary"): false);
				
				if (data != null) {
					request.sendResponse(Response.getInstance(200, Response.OK, data.toString()));
				}
				else {
					request.sendResponse(Response.getInstance(400, Response.BADREQUEST,
						new JSONObject().put("error", "database not found").toString()));
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
