package com.itahm.command;

import java.io.IOException;

import com.itahm.json.JSONException;
import com.itahm.json.JSONObject;

import com.itahm.Agent;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class Message implements Command {

	@Override
	public Response execute(Request request, JSONObject data) throws IOException {
		try {
			Agent.manager.gcmm.broadcast(data.getString("message"));
			
			return Response.getInstance(request, Response.Status.OK);
		}
		catch(NullPointerException npe) {
			return Response.getInstance(request, Response.Status.UNAVAILABLE);
		}
		catch (JSONException jsone) {
			return Response.getInstance(request, Response.Status.BADREQUEST,
					new JSONObject().put("error", "invalid json request").toString());
		}
	}

}