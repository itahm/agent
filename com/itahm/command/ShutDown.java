package com.itahm.command;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class ShutDown extends Command {

	@Override
	protected void execute(Request request, JSONObject data) throws IOException {
		request.sendResponse(Response.getInstance(200, Response.OK, ""));
		
		ITAhM.shutdown();
	}

}
