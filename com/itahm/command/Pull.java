package com.itahm.command;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.table.Table;
import com.itahm.ITAhM;
import com.itahm.http.Response;

public class Pull extends Command {
	
	public Pull() {
	}

	@Override
	protected void execute(JSONObject request, Response response) throws IOException {
		try {
			Table table = ITAhM.getTable(request.getString("database"));
			
			if (table == null) {
				response.badRequest(new JSONObject().put("error", "database not found"));
			}
			else {
				response.ok(table.getJSONObject());
			}
		}
		catch (JSONException jsone) {
			response.badRequest(new JSONObject().put("error", "invalid json request"));
		}
	}
	
}
