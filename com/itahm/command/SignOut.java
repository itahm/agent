package com.itahm.command;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.http.Session;

public class SignOut extends Command {

	public Response execute(Request request, JSONObject data, Session session) throws IOException {
		if (session != null) {
			session.close();
		}
		
		return execute(request, data);
	}
		
	@Override
	protected Response execute(Request request, JSONObject data) throws IOException {
		return Response.getInstance(Response.Status.OK);
	}

}
