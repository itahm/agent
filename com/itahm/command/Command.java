package com.itahm.command;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.http.Session;

public abstract class Command {
	public Response execute(Request request, JSONObject data, Session session) throws IOException {
		return session == null? Response.getInstance(Response.Status.UNAUTHORIZED): execute(request, data);
	}
	
	protected abstract Response execute(Request request, JSONObject data) throws IOException;
}
