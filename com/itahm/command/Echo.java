package com.itahm.command;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.Constant;
import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.http.Session;

public class Echo extends Command {
	
	private int level;
	
	public Echo() {
	}

	public void execute(Request request, JSONObject data, Session session) throws IOException {
		if (session != null) {
			this.level = (Integer)session.getExtras();
		}
		
		super.execute(request, data, session);
	}
	
	@Override
	protected void execute(Request request, JSONObject data) throws IOException {
		request.sendResponse(Response.getInstance(200, Response.OK, new JSONObject()
			.put(Constant.STRING_LEVEL, this.level).toString()));
	}

}
