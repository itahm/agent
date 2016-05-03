package com.itahm.command;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.http.Session;

public abstract class Command {
	public void execute(Request request, JSONObject data, Session session) throws IOException {
		if (session == null) {
			request.sendResponse(Response.getInstance(401, Response.UNAUTHORIZED, ""));
		}
		else {
			execute(request, data);
		}
	}
	
	protected abstract void execute(Request request, JSONObject data) throws IOException;
}
