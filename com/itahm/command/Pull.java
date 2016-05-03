package com.itahm.command;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.table.Table;
import com.itahm.ITAhM;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class Pull extends Command {
	
	@Override
	protected void execute(Request request, JSONObject data) throws IOException {
		try {
			Table table = ITAhM.getTable(data.getString("database"));
			
			if (table == null) {
				request.sendResponse(Response.getInstance(400, Response.BADREQUEST,
					new JSONObject().put("error", "database not found").toString()));
			}
			else {
				data = new JSONObject();
				
				data.put("sequence", table.lock());
				data.put("data", table.getJSONObject());
				
				request.sendResponse(Response.getInstance(200, Response.OK, data.toString()));
			}
		}
		catch (JSONException jsone) {
			request.sendResponse(Response.getInstance(400, Response.BADREQUEST,
				new JSONObject().put("error", "invalid json request").toString()));
		}
	}
	
}
