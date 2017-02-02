package com.itahm.command;

import java.io.IOException;

import com.itahm.json.JSONObject;

import com.itahm.http.Request;
import com.itahm.http.Response;

public class ShutDown implements Command {

	@Override
	public Response execute(Request request, JSONObject data) throws IOException {
		System.out.println("shutdown command.");
		
		return Response.getInstance(request, Response.Status.OK);
	}

}
