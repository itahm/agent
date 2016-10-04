package com.itahm.command;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.SNMPNode;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class Select extends Command {
	
	@Override
	protected Response execute(Request request, JSONObject data) throws IOException {
		try {
			SNMPNode node = ITAhM.agent.getNode(data.getString("ip"));
			
			if (node != null) {
				return Response.getInstance(Response.Status.OK, node.getData().toString());
			}
			else {
				return Response.getInstance(Response.Status.BADREQUEST,
					new JSONObject().put("error", "node not found").toString());
			}
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
