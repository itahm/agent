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
		if (!"HTTP/1.1".equals(request.getRequestVersion())) {
			request.sendResponse(Response.getInstance(Response.Status.VERSIONNOTSUP));
		}
		else {
			Response response = null;
			
			switch(request.getRequestMethod()) {
			case "OPTIONS":
				response = Response.getInstance(Response.Status.OK).setResponseHeader("Allow", "OPTIONS, GET, POST");
			
				break;
			
			case"POST":
				try {
					JSONObject data = new JSONObject(new String(request.getRequestBody(), StandardCharsets.UTF_8.name()));
					
					Session session = getSession(request);
					
					if (!data.has("command")) {
						response = Response.getInstance(Response.Status.BADREQUEST, new JSONObject().put("error", "command not found").toString());
					}
					else {
						Command command = Commander.getCommand(data.getString("command"));
						
						if (command != null) {
							response = command.execute(request, data, session);
						}
						else {
							response = Response.getInstance(Response.Status.BADREQUEST, new JSONObject().put("error", "invalid command").toString());
						}
					}
				}
				catch (JSONException jsone) {
					response = Response.getInstance(Response.Status.BADREQUEST, new JSONObject().put("error", "invalid json request").toString());
				}
				
				break;
			
			case "GET":
				File uri = new File(ITAhM.getRoot().getParentFile(), request.getRequestURI());
				
				if (uri.isFile()) {
					response = Response.getInstance(Response.Status.OK, uri);
				}
				
				if (response == null) {
					response = Response.getInstance(Response.Status.NOTFOUND);
				}
				
				break;
			default:
				response = Response.getInstance(Response.Status.NOTALLOWED).setResponseHeader("Allow", "OPTIONS, POST, GET");
			}
			
			if (response != null) { /* listen인 경우 null*/
				request.sendResponse(response);
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
