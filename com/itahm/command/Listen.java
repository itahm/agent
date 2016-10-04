package com.itahm.command;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class Listen extends Command {
	
	@Override
	protected Response execute(Request request, JSONObject data) throws IOException {
		long index = data.has("index")? data.getInt("index"): -1;
		
		ITAhM.log.listen(request, index);
		
		return null;
	}
	
}
