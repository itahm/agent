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

	public void execute(Request request, Response response) throws IOException {
		Session session = request.session();
		
		if (session != null) {
			this.level = session.getLevel();
		}
		
		super.execute(request, response);
	}
	
	@Override
	protected void execute(JSONObject data, Response response) throws IOException {
		response.ok(new JSONObject().put(Constant.STRING_LEVEL, this.level));
	}

}
