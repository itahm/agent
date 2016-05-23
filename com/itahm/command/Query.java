package com.itahm.command;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.SNMPNode;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class Query extends Command {
	
	@Override
	protected void execute(Request request, JSONObject data) throws IOException {
		
		try {
			SNMPNode node = ITAhM.snmp.getNode(data.getString("ip"));
			
			if (node != null) {
				data = node.getData(data.getString("database")
					, String.valueOf(data.getInt("index"))
					, data.getLong("start")
					, data.getLong("end")
					, data.has("summary")? data.getBoolean("summary"): false);
				
				if (data != null) {
					request.sendResponse(Response.getInstance(200, Response.OK, data));
				}
				else {
					request.sendResponse(Response.getInstance(400, Response.BADREQUEST,
						new JSONObject().put("error", "database not found")));
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
