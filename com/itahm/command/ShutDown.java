package com.itahm.command;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.http.Response;

public class ShutDown extends Command {

	@Override
	protected void execute(JSONObject data, Response response) throws IOException {
		response.ok();
		
		ITAhM.shutdown();
	}

}
