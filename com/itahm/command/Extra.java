package com.itahm.command;

import java.io.IOException;

import com.itahm.json.JSONException;
import com.itahm.json.JSONObject;

import com.itahm.Agent;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class Extra implements Command {
	
	public enum Key {
		RESET,
		FAILURE;
	};
	
	@Override
	public Response execute(Request request, JSONObject data) throws IOException {
		
		try {
			switch(Key.valueOf(data.getString("extra").toUpperCase())) {
			case RESET:
				Agent.manager.snmp.resetResponse(data.getString("ip"));
				
				return Response.getInstance(Response.Status.OK);
			case FAILURE:
				JSONObject json = Agent.manager.snmp.getFailureRate(data.getString("ip"));
				
				if (json == null) {
					return Response.getInstance(Response.Status.BADREQUEST,
						new JSONObject().put("error", "node not found").toString());
				}
				
				return Response.getInstance(Response.Status.OK, json.toString());
			}
		}
		catch (JSONException jsone) {
			return Response.getInstance(Response.Status.BADREQUEST,
					new JSONObject().put("error", "invalid json request").toString());
		}
		catch(IllegalArgumentException iae) {
		}
		
		return Response.getInstance(Response.Status.BADREQUEST,
			new JSONObject().put("error", "invalid extra").toString());
	}

}
