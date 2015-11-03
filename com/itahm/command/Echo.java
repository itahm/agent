package com.itahm.command;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.http.Response;

public class Echo extends Command {
	
	public Echo() {
	}

	@Override
	protected void execute(JSONObject data, Response response) throws IOException {
		response.status(200, "OK").send();
	}

}
