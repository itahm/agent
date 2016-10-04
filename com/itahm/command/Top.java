package com.itahm.command;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class Top extends Command {
	
	private static int TOP_MAX = 10;
	
	@Override
	protected Response execute(Request request, JSONObject data) throws IOException {
		int count = TOP_MAX;
		
		try {
			if (data.has("count")) {
				count = Math.min(data.getInt("count"), TOP_MAX);
			}
			
			return Response.getInstance(Response.Status.OK, ITAhM.agent.getTop(count).toString());
		}
		catch(NullPointerException npe) {
			return Response.getInstance(Response.Status.UNAVAILABLE);
		}
		catch (JSONException jsone) {
			return Response.getInstance(Response.Status.BADREQUEST,
					new JSONObject().put("error", "invalid json request").toString());
		}
	}
	
}
