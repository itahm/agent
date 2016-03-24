package com.itahm.command;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.http.Response;
import com.itahm.snmp.Node;
import com.itahm.snmp.NodeList;

public class Select extends Command {
	
	public Select() {
	}

	@Override
	protected void execute(JSONObject request, Response response) throws IOException {		
		try {
			Node node = NodeList.getNode(request.getString("ip"));
			
			if (node != null) {
				response.ok(node.getData());
				
				if (request.has("trigger") && request.getBoolean("trigger")) {
					//TODO realtime 추후 구현.
					//ITAhM.getSnmp().request(node);
				}
			}
			else {
				response.badRequest(new JSONObject().put("error", "node not found"));
			}
		}
		catch (JSONException jsone) {
			response.badRequest(new JSONObject().put("error", "invalid json request"));
		}
		catch (IllegalArgumentException iae) {
			response.badRequest(new JSONObject().put("error", "database not found"));
		}
	}

	
}
