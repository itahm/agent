package com.itahm.command;

import java.io.IOException;

import com.itahm.json.JSONException;
import com.itahm.json.JSONObject;

import com.itahm.table.Table;
import com.itahm.Agent;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class Config implements Command {
	
	@Override
	public Response execute(Request request, JSONObject data) throws IOException {
		try {
			Table table = Agent.getTable(Table.CONFIG);
			
			switch(data.getString("key")) {
			case "clean":
				int clean = data.getInt("value");
				
				table.getJSONObject().put("clean", clean);
				table.save();
				
				Agent.snmp.clean(clean);
				
				break;
			
			case "dashboard":
				table.getJSONObject().put("dashboard", data.getJSONObject("value"));
				table.save();
				
				break;

			case "display":
				table.getJSONObject().put("display", data.getString("value"));
				table.save();
				
				break;

			default:
				return Response.getInstance(request, Response.Status.BADREQUEST,
					new JSONObject().put("error", "invalid config parameter").toString());
			}
			
			return Response.getInstance(request, Response.Status.OK);
		}
		catch (JSONException jsone) {
			return Response.getInstance(request, Response.Status.BADREQUEST,
				new JSONObject().put("error", "invalid json request").toString());
		}
	}
	
}
