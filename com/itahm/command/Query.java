package com.itahm.command;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.http.Response;
import com.itahm.snmp.Node;
import com.itahm.snmp.NodeList;

public class Query extends Command {
	
	public Query() {
	}

	@Override
	protected void execute(JSONObject request, Response response) throws IOException {
		
		try {
			Node node = NodeList.getNode(request.getString("ip"));
			
			if (node != null) {
				JSONObject data = node.getData(request.getString("database")
					, Integer.toString(request.getInt("index"))
					, request.getLong("start")
					, request.getLong("end")
					, request.has("summary")? request.getBoolean("summary"): false);
				
				if (data != null) {
					response.ok(data);
				}
				else {
					response.badRequest(new JSONObject().put("error", "database not found"));
				}
			}
			else {
				response.badRequest(new JSONObject().put("error", "node not found"));
			}
		}
		catch (JSONException | IllegalArgumentException e) {
			response.badRequest(new JSONObject().put("error", "invalid json request"));
		}
	}

}
