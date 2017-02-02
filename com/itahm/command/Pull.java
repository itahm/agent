package com.itahm.command;

import java.io.IOException;

import com.itahm.json.JSONException;
import com.itahm.json.JSONObject;

import com.itahm.table.Table;
import com.itahm.Agent;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class Pull implements Command {
	
	@Override
	public Response execute(Request request, JSONObject data) throws IOException {
		try {
			Table table = Agent.getTable(data.getString("database"));
			
			if (table == null) {
				return Response.getInstance(request, Response.Status.BADREQUEST,
					new JSONObject().put("error", "database not found").toString());
			}
			else {
				return Response.getInstance(request, Response.Status.OK, table.getJSONObject().toString());
			}
		}
		catch (JSONException jsone) {
			return Response.getInstance(request, Response.Status.BADREQUEST,
				new JSONObject().put("error", "invalid json request").toString());
		}
	}
	
}
