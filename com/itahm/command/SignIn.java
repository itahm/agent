package com.itahm.command;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.session.Session;

public class SignIn extends Command {
	
	public SignIn() {
	}

	public void execute(Request request, Response response) throws IOException {
		execute(request.getJSONObject(), response);
	}
		
	@Override
	protected void execute(JSONObject request, Response response) throws IOException {
		try {
			JSONObject accountData = ITAhM.getTable("account").getJSONObject();
			String username = request.getString("username");
			String password = request.getString("password");
			
			if (accountData.has(username) && accountData.getJSONObject(username).getString("password").equals(password)) {
				// signin 성공, cookie 발행
				Session session = Session.getInstance();
				
				response.header("Set-Cookie", String.format(Response.COOKIE, session.getID())).ok();
			}
			else {
				response.unauthorized();
			}
		}
		catch (JSONException jsone) {
			response.badRequest(new JSONObject().put("error", "invalid json request"));
		}
	}

}
