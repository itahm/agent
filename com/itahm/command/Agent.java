package com.itahm.command;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class Agent extends Command {
	
	@Override
	protected Response execute(Request request, JSONObject data) throws IOException {
		try {
			return Response.getInstance(Response.Status.OK,
				new JSONObject()
				.put("connections", ITAhM.http.getConnectionSize())
				.put("space", ITAhM.getRoot().getUsableSpace())
				.put("waiters", ITAhM.log.getWaiterCount()).toString());
		}
		catch (NullPointerException npe) {
			return Response.getInstance(Response.Status.UNAVAILABLE);
		}
	}
	
}
