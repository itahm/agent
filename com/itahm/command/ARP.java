package com.itahm.command;

import java.io.IOException;

import com.itahm.json.JSONObject;

import com.itahm.Agent;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class ARP implements Command {
	
	@Override
	public Response execute(Request request, JSONObject data) throws IOException {
		try {
			return Response.getInstance(Response.Status.OK, Agent.manager.snmp.getARP().toString());
		}
		catch (NullPointerException npe) {
			return Response.getInstance(Response.Status.UNAVAILABLE);
		}
	}
	
}
