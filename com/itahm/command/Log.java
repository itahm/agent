package com.itahm.command;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class Log extends Command {
	
	public Log() {
	}
	
	@Override
	protected void execute(Request request, JSONObject data) throws IOException {
		request.sendResponse(Response.getInstance(200, Response.OK, ITAhM.log.read(data.getLong("date"))));
	}

}
