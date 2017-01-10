package com.itahm.command;

import java.io.IOException;

import com.itahm.json.JSONObject;

import com.itahm.Agent;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class Log implements Command {
	
	@Override
	public Response execute(Request request, JSONObject data) throws IOException {
		return Response.getInstance(Response.Status.OK, Agent.manager.log.read(data.getLong("date")));
	}

}
