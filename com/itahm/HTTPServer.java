package com.itahm;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.command.Command;
import com.itahm.http.Listener;
import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.http.Session;

public class HTTPServer extends Listener {
	
	public HTTPServer(String ip, int tcp) throws IOException {
		super(ip, tcp);
	}
	
	@Override
	protected void onStart() {
		System.out.println("HTTP Server running...");
	}
	
	@Override
	protected void onStop() {
		System.out.println("stop HTTP Server.");
	}

	@Override
	protected void onRequest(Request request) {
		try {
			processRequest(request);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onClose(Request request, boolean closed) {
		Event.cancel(request);
	}
	
	private void processRequest(Request request) throws IOException{
		String method = request.getRequestMethod();
		
		if (!"HTTP/1.1".equals(request.getRequestVersion())) {
			request.sendResponse(Response.getInstance(505, Response.VERSIONNOTSUP, ""));
		}
		else {
			if ("OPTIONS".equals(method)) {
				request.sendResponse(Response.getInstance(200, "OK", "")
					.setResponseHeader("Allow", "OPTIONS, POST"));
			}
			else if ("POST".equals(method)/* || "GET".equals(method)*/) {
				String body = new String(request.getRequestBody(), "UTF-8");
				JSONObject data = null;
				try {
					data = new JSONObject(body);
				}
				catch (JSONException jsone) {
				}
				
				if (data != null) {
					Session session = getSession(request);
					
					if (data.has("command")) {
						Command command = Commander.getCommand(data.getString("command"));
						
						if (command != null) {
							command.execute(request, data, session);
							
							return;
						}
					}
				}
				
				request.sendResponse(Response.getInstance(400, "Bad Request", ""));
			}
			else {
				request.sendResponse(Response.getInstance(400, "Bad Request", "")
					.setResponseHeader("Allow", "OPTIONS, POST"));
			}
		}
	}
	
	private static Session getSession(Request request) {
		String cookie = request.getRequestHeader("cookie");
		
		if (cookie == null) {
			return null;
		}
		
		String [] cookies = cookie.split("; ");
		String [] token;
		Session session = null;
		
		for(int i=0, length=cookies.length; i<length; i++) {
			token = cookies[i].split("=");
			
			if (token.length == 2 && "SESSION".equals(token[0])) {
				session = Session.find(token[1]);
				
				if (session != null) {
					session.update();
				}
			}
		}
		
		return session;
	}
}
