package com.itahm.command;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class ShutDown implements Command {

	@Override
	public Response execute(Request request, JSONObject data) throws IOException {
		ITAhM.shutdown();
		
		return Response.getInstance(Response.Status.OK);
	}

}
