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

	public Response execute(Request request, JSONObject data, Session session) throws IOException {
		return execute(request, data);
	}
	
	@Override
	protected Response execute(Request request, JSONObject data) throws IOException {
		try {
			String id = data.getString(GCMManager.ID);
			
			if (id.length() == 0) {
				throw new JSONException("");
			}
			
			ITAhM.gcmm.register(id, data.getString(GCMManager.TOKEN));
			
			return Response.getInstance(Response.Status.OK);
		}
		catch (JSONException jsone) {
			return Response.getInstance(Response.Status.BADREQUEST,
					new JSONObject().put("error", "invalid json request").toString());
		}
	}

}
