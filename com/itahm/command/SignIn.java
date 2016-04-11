package com.itahm.command;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.Constant;
import com.itahm.ITAhM;
import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.http.Session;

public class SignIn extends Command {
	
	public SignIn() {
	}

	public void execute(Request request, Response response) throws IOException {
		execute(request.getJSONObject(), response);
	}
		
	@Override
	protected void execute(JSONObject request, Response response) throws IOException {
		try {
			String username = request.getString(Constant.STRING_USER);
			String password = request.getString(Constant.STRING_PWORD);
			JSONObject accountData = ITAhM.getTable("account").getJSONObject();
			
			if (accountData.has(username)) {
				 JSONObject account = accountData.getJSONObject(username);
				 
				 if (account.getString(Constant.STRING_PWORD).equals(password)) {
					// signin 성공, cookie 발행
					Session session = Session.getInstance(account.getInt(Constant.STRING_LEVEL));
					
					response.header("Set-Cookie", String.format(Response.COOKIE, session.getID()))
						.ok(new JSONObject()
							.put(Constant.STRING_LEVEL, session.getLevel()));
					
					return;
				 }
			}
			
			response.unauthorized();
		}
		catch (JSONException jsone) {
			response.badRequest(new JSONObject().put(Constant.STRING_ERROR, "invalid json request"));
		}
	}

}
