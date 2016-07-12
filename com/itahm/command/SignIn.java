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
	
	public final static String COOKIE = "SESSION=%s; HttpOnly";
	
	public SignIn() {
	}

	public void execute(Request request, JSONObject data, Session session) throws IOException {
		execute(request, data);
	}
		
	@Override
	protected void execute(Request request, JSONObject data) throws IOException {
		try {
			String username = data.getString(Constant.STRING_USER);
			String password = data.getString(Constant.STRING_PWORD);
			JSONObject accountData = ITAhM.getTable("account").getJSONObject();
			
			if (accountData.has(username)) {
				 JSONObject account = accountData.getJSONObject(username);
				 
				 if (account.getString(Constant.STRING_PWORD).equals(password)) {
					// signin 성공, cookie 발행
					Session session = Session.getInstance(account.getInt(Constant.STRING_LEVEL));
					
					request.sendResponse(Response.getInstance(200, Response.OK,
						new JSONObject().put(Constant.STRING_LEVEL, (Integer)session.getExtras()).toString())
							.setResponseHeader("Set-Cookie", String.format(COOKIE, session.getCookie())));
					
					return;
				 }
			}
			
			request.sendResponse(Response.getInstance(401, Response.UNAUTHORIZED));
		}
		catch (JSONException jsone) {
			request.sendResponse(Response.getInstance(400, Response.BADREQUEST,
					new JSONObject().put("error", "invalid json request").toString()));
		}
	}

}
