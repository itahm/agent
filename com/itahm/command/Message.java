package com.itahm.command;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class Message extends Command {

	@Override
	protected void execute(Request request, JSONObject data) throws IOException {
		try {
			ITAhM.gcmm.broadcast(data.getString("message"));
			
			request.sendResponse(Response.getInstance(200, Response.OK, ""));
		}
		catch (JSONException jsone) {
			request.sendResponse(Response.getInstance(400, Response.BADREQUEST,
					new JSONObject().put("error", "invalid json request").toString()));
		}
	}

}