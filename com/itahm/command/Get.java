package com.itahm.command;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.Data;
import com.itahm.http.Response;

public class Get extends Command {
	
	public Get() {
	}

	@Override
	protected void execute(JSONObject request, Response response) throws IOException {
		try {
			JSONObject data = Data.getJSONObject(request.getString("database"));
			
			if (data != null) {
				response.ok(data);
				
				return;
			}
		}
		catch (JSONException jsone) {
		}
		
		response.badRequest();
	}

}
