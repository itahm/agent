package com.itahm.command;

import java.io.IOException;

import com.itahm.json.JSONObject;

import com.itahm.Agent;
import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.table.Table;

public class Config implements Command {
	
	@Override
	public Response execute(Request request, JSONObject data) throws IOException {
		Table table = Agent.getTable(Table.CONFIG);
		JSONObject config = table.getJSONObject();
		
		if (data.has("timeout")) {
			int timeout = data.getInt("timeout");
			
			config.put("timeout", timeout);
		}
		else {
		}
		
		table.save();
		
		return Response.getInstance(Response.Status.OK, config.toString());
	}

}
