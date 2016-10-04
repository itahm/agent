package com.itahm.command;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.http.Session;

public class UnRegister extends Command {

	public Response execute(Request request, JSONObject data, Session session) throws IOException {
		return execute(request, data);
	}
	
	@Override
	protected Response execute(Request request, JSONObject data) throws IOException {
		try {
			String token = data.getString("token");
			
			if (token.length() == 0) {
				return Response.getInstance(Response.Status.BADREQUEST,
					new JSONObject().put("error", "invalid token").toString());
			}
			
			ITAhM.gcmm.onUnRegister(token);
			
			return Response.getInstance(Response.Status.OK);
		}
		catch (JSONException jsone) {
			return Response.getInstance(Response.Status.BADREQUEST,
				new JSONObject().put("error", "invalid json request").toString());
		}
	}

}
