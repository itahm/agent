package com.itahm.command;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class Message implements Command {

	@Override
	public Response execute(Request request, JSONObject data) throws IOException {
		try {
			ITAhM.gcmm.broadcast(data.getString("message"));
			
			return Response.getInstance(Response.Status.OK);
		}
		catch(NullPointerException npe) {
			return Response.getInstance(Response.Status.UNAVAILABLE);
		}
		catch (JSONException jsone) {
			return Response.getInstance(Response.Status.BADREQUEST,
					new JSONObject().put("error", "invalid json request").toString());
		}
	}

}