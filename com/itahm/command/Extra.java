package com.itahm.command;

import java.io.IOException;

import com.itahm.json.JSONObject;

import com.itahm.Agent;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class Extra implements Command {
	
	private final static String RESET_RESPONSE = "reset";
	
	@Override
	public Response execute(Request request, JSONObject data) throws IOException {
		
		switch(data.getString("extra")) {
		case RESET_RESPONSE:
			Agent.manager.snmp.resetResponse(data.getString("ip"));
			
			break;
		}
		
		return Response.getInstance(Response.Status.OK);
	}

}
