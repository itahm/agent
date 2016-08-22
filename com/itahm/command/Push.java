package com.itahm.command;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.table.Table;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class Push extends Command {
	
	@Override
	protected void execute(Request request, JSONObject data) throws IOException {
		try {
			Table table = ITAhM.getTable(data.getString("database"));
			
			if (table == null) {
				request.sendResponse(Response.getInstance(400, Response.BADREQUEST,
					new JSONObject().put("error", "database not found")));
			}
			else {
				if (table.commit(data.getInt("sequence"))) {
					data = table.save(data.getJSONObject("data"));
					
					request.sendResponse(Response.getInstance(200, Response.OK,
						new JSONObject()
							.put("sequence", table.lock())
							.put("data", data)));
				}
				else {
					request.sendResponse(Response.getInstance(409, Response.CONFLICT,
						new JSONObject().put("error", "database lock")));
				}
			}
		}
		catch (JSONException jsone) {jsone.printStackTrace();
			request.sendResponse(Response.getInstance(400, Response.BADREQUEST,
				new JSONObject().put("error", "invalid json request")));
		}
	}
	
}
