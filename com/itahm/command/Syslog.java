package com.itahm.command;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class Syslog implements Command {
	
	@Override
	public Response execute(Request request, JSONObject data) throws IOException {
		return Response.getInstance(Response.Status.OK, ITAhM.agent.syslog.getLog(data.getLong("date")));
	}

}