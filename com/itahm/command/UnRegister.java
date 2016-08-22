package com.itahm.command;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.http.Session;

public class UnRegister extends Command {

	public void execute(Request request, JSONObject data, Session session) throws IOException {
		execute(request, data);
	}
	
	@Override
	protected void execute(Request request, JSONObject data) throws IOException {
		try {
			String token = data.getString("token");
			
			if (token.length() == 0) {
				throw new JSONException("");
			}
			
			ITAhM.gcmm.onUnRegister(token);
			
			request.sendResponse(Response.getInstance(200, Response.OK, ""));
		}
		catch (JSONException jsone) {
			request.sendResponse(Response.getInstance(400, Response.BADREQUEST,
				new JSONObject().put("error", "invalid json request")));
		}
	}

}
