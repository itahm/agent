package com.itahm.command;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class Top extends Command {
	
	private static int TOP_MAX = 10;
	
	@Override
	protected void execute(Request request, JSONObject data) throws IOException {
		int count = TOP_MAX;
		
		try {
			if (data.has("count")) {
				count = Math.min(data.getInt("count"), TOP_MAX);
			}
			
			request.sendResponse(Response.getInstance(200, Response.OK, ITAhM.agent.getTop(count)));
		}
		catch (JSONException jsone) {
			request.sendResponse(Response.getInstance(400, Response.BADREQUEST,
					new JSONObject().put("error", "invalid json request")));
		}
	}
	
}
