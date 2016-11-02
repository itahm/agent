package com.itahm.command;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.table.Table;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class Put implements Command {
	
	@Override
	public Response execute(Request request, JSONObject data) throws IOException {
		try {
			Table table = ITAhM.getTable(data.getString("database"));
			
			if (table == null) {
				return Response.getInstance(Response.Status.BADREQUEST,
					new JSONObject().put("error", "database not found").toString());
			}
			else {
				return Response.getInstance(Response.Status.OK,
					table.put(data.getString("key"), data.isNull("value")? null: data.getJSONObject("value")).toString());
			}
		}
		catch (JSONException jsone) {
			return Response.getInstance(Response.Status.BADREQUEST,
				new JSONObject().put("error", "invalid json request").toString());
		}
	}
	
}
