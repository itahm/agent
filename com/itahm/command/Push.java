package com.itahm.command;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.table.Table;
import com.itahm.http.Response;

public class Push extends Command {
	
	public Push() {
	}

	@Override
	protected void execute(JSONObject request, Response response) throws IOException {
		try {
			Table table = ITAhM.getTable(request.getString("database"));
			
			if (table == null) {
				response.badRequest(new JSONObject().put("error", "database not found"));
			}
			else {
				if (table.commit(request.getInt("sequence"))) {
					table.save(request.getJSONObject("data"));
					
					response.ok();
				}
				else {
					response.badRequest(new JSONObject().put("error", "database lock"));
				}
			}
		}
		catch (JSONException jsone) {
			response.badRequest(new JSONObject().put("error", "invalid json request"));
		}
		catch (IllegalArgumentException iae) {
			response.badRequest(new JSONObject().put("error", "database not found"));
		}
	}
	
}
