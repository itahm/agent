package com.itahm;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.command.Command;
import com.itahm.command.Commander;
import com.itahm.http.Listener;
import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.http.Session;

public class HTTPServer extends Listener {
	
	public HTTPServer(String ip, int tcp) throws IOException {
		super(ip, tcp);
	}
	
	public void close() {
		try {
			super.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void onStart() {
		System.out.println("HTTP Server running...");
	}
	
	@Override
	protected void onStop() {
		System.out.println("Stop HTTP Server.");
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
	protected void onClose(Request request) {
		ITAhM.log.cancel(request);
	}
	
	private void processRequest(Request request) throws IOException{
		String method = request.getRequestMethod();
		
		if (!"HTTP/1.1".equals(request.getRequestVersion())) {
			request.sendResponse(Response.getInstance(505, Response.VERSIONNOTSUP, ""));
		}
		else {
			if ("OPTIONS".equals(method)) {
				request.sendResponse(Response.getInstance(200, "OK").setResponseHeader("Allow", "OPTIONS, GET, POST"));
			}
			else if ("POST".equals(method)) {
				try {
					JSONObject data = new JSONObject(new String(request.getRequestBody(), StandardCharsets.UTF_8.name()));
					
					Session session = getSession(request);
					
					if (!data.has("command")) {
						request.sendResponse(Response.getInstance(400, "Bad Request", new JSONObject().put("error", "command not found")));
					}
					else {
						Command command = Commander.getCommand(data.getString("command"));
						
						if (command != null) {
							command.execute(request, data, session);
						}
						else {
							request.sendResponse(Response.getInstance(400, "Bad Request", new JSONObject().put("error", "invalid command")));
						}
					}
				}
				catch (JSONException jsone) {
					request.sendResponse(Response.getInstance(400, Response.BADREQUEST, new JSONObject().put("error", "invalid json request")));
				}
			}
			else if ("GET".equals(method)) {
				File uri = new File(ITAhM.getRoot().getParentFile(), request.getRequestURI());
				
				if (uri.isFile()) {
					Response response = Response.getInstance(200, "OK", uri);
					if (response != null) {
						request.sendResponse(response);
						
						return;
					}
				}
				
				request.sendResponse(Response.getInstance(404, "Not Found"));
			}
			else {
				request.sendResponse(Response.getInstance(405, "Method Not Allowed").setResponseHeader("Allow", "OPTIONS, POST"));
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
