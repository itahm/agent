package com.itahm.command;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.Data;
import com.itahm.Data.Table;
import com.itahm.http.Response;

public class Select extends Command {
	
	public Select() {
	}

	@Override
	protected void execute(JSONObject request, Response response) throws IOException {
		try {
			String database = request.getString("database");
			Table table;
			
			if ("snmp".equals(database)) {
				table = Table.DEVICE;
			}
			else {
				table = Table.valueOf(request.getString("device"));;
			}
			
			response.ok();
		}
		catch (JSONException jsone) {
			response.badRequest(new JSONObject().put("error", "invalid json request"));
		}
		catch (IllegalArgumentException iae) {
			response.badRequest(new JSONObject().put("error", "database not found"));
		}
	}

	
}
