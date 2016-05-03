package com.itahm.command;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.http.Session;

public class SignOut extends Command {
	
	public SignOut() {
	}
	
	public void execute(Request request, JSONObject data, Session session) throws IOException {
		if (session != null) {
			session.close();
		}
		
		request.sendResponse(Response.getInstance(200, Response.OK, ""));
	}
		
	@Override
	protected void execute(Request request, JSONObject data) throws IOException {
	}

}
