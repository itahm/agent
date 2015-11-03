package com.itahm.command;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.session.Session;

public abstract class Command {
	public void execute(Request request, Response response) throws IOException {
		Session session = request.session();
		
		if (session == null) {
			response.unauthorized();
		}
		else {
			try {
				execute(request.getJSONObject(), response);
			}
			catch (JSONException jsone) {
				response.badRequest();
			}
		}
	}
	
	protected abstract void execute(JSONObject data, Response response) throws IOException;
}
