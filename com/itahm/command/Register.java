package com.itahm.command;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.GCMManager;
import com.itahm.ITAhM;
import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.http.Session;

public class Register extends Command {

	public void execute(Request request, JSONObject data, Session session) throws IOException {
		execute(request, data);
	}
	
	@Override
	protected void execute(Request request, JSONObject data) throws IOException {
		try {
			String id = data.getString(GCMManager.ID);
			
			if (id.length() == 0) {
				throw new JSONException("");
			}
			
			ITAhM.gcmm.register(id, data.getString(GCMManager.TOKEN));
			
			request.sendResponse(Response.getInstance(200, Response.OK, data.toString()));
		}
		catch (JSONException jsone) {
			request.sendResponse(Response.getInstance(400, Response.BADREQUEST,
					new JSONObject().put("error", "invalid json request").toString()));
		}
	}

}
