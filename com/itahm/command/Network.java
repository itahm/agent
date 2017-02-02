package com.itahm.command;

import java.io.IOException;

import com.itahm.json.JSONObject;

import com.itahm.Agent;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class Network implements Command {
	
	@Override
	public Response execute(Request request, JSONObject data) throws IOException {
		try {
			return Response.getInstance(request, Response.Status.OK, Agent.manager.snmp.getNetwork().toString());
		}
		catch (NullPointerException npe) {
			return Response.getInstance(request, Response.Status.UNAVAILABLE);
		}
	}
	
}
