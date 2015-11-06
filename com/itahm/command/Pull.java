package com.itahm.command;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.Data;
import com.itahm.http.Response;

public class Pull extends Command {
	
	public Pull() {
	}

	@Override
	protected void execute(JSONObject request, Response response) throws IOException {
		try {
			JSONObject data = Data.getJSONObject(request.getString("database"));
			
			if (data != null) {
				response.ok(data);
			}
			else {
				response.badRequest(new JSONObject().put("error", "database not found").toString());
			}
		}
		catch (JSONException jsone) {
			response.badRequest(new JSONObject().put("error", "invalid json request").toString());
		}
	}

}
