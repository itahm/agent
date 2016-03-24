package com.itahm.command;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.event.Event;
import com.itahm.http.Response;

public class Listen extends Command {
	
	private final static String STRING_INDEX = "index";
	
	public Listen() {
	}

	@Override
	protected void execute(JSONObject request, Response response) throws IOException {
		int index = -1;
		
		if (request.has(STRING_INDEX)) {
			index = request.getInt("index");
		}
		
		Event.listen(response, index);
	}

}
