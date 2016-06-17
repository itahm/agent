package com.itahm.command;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class Listen extends Command {
	
	public Listen() {
	}

	@Override
	protected void execute(Request request, JSONObject data) throws IOException {
		
		try {
			long index = data.has("index")? data.getInt("index"): -1;
		
			ITAhM.log.listen(request, index);
		}
		catch (JSONException jsone) {
			request.sendResponse(Response.getInstance(400, Response.BADREQUEST, ""));
		}
	}

}
