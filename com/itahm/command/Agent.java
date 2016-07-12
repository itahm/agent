package com.itahm.command;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class Agent extends Command {
	
	@Override
	protected void execute(Request request, JSONObject data) throws IOException {
		JSONObject response = new JSONObject();
		
		response
			.put("connections", ITAhM.http.getConnectionSize())
			.put("space", ITAhM.getRoot().getUsableSpace());
		
		request.sendResponse(Response.getInstance(200, Response.OK, response));
	}
	
}
