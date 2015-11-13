package com.itahm.command;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.session.Session;

public class SignOut extends Command {
	
	public SignOut() {
	}
	
	public void execute(Request request, Response response) throws IOException {
		Session session = request.session();
		
		if (session != null) {
			session.close();
		}
		
		response.status(401, "Unauthorized").send();
	}
		
	@Override
	protected void execute(JSONObject request, Response response) throws IOException {
	}

}
