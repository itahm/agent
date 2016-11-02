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
					
					if (!data.has("command")) {
						response = Response.getInstance(Response.Status.BADREQUEST, new JSONObject().put("error", "command not found").toString());
					}
					else {
						String cmd = data.getString("command");
						Session session = getSession(request);
						
						if ("echo".equals(cmd)) {
							response = Response.getInstance(Response.Status.OK, new JSONObject()
									.put("level", session == null? -1: (int)session.getExtras()).toString());
						}
						else if ("signin".equals(cmd)) {
							if (session == null) {
								session = signIn(data);
							}
							
							if (session == null) {
								response = Response.getInstance(Response.Status.UNAUTHORIZED);
							}
							else {
								response = Response.getInstance(Response.Status.OK,	new JSONObject()
									.put("level", (int)session.getExtras())
									.put("version", ITAhM.VERSION).toString())
										.setResponseHeader("Set-Cookie", String.format("SESSION=%s; HttpOnly", session.getCookie()));
							}
						}
						else if ("signout".equals(cmd)) {
							if (session != null) {
								session.close();
							}
							
							response = Response.getInstance(Response.Status.OK);
						}
						else {
							Command command = Commander.getCommand(cmd);
							
							if (command != null) {
								if ("put".equals(cmd) && "gcm".equals(data.getString("database"))) {
									response = command.execute(request, data);
								}
								else if (session != null) {
									response = command.execute(request, data);
								}
								else {
									response = Response.getInstance(Response.Status.UNAUTHORIZED);
								}
							}
							else {
								response = Response.getInstance(Response.Status.BADREQUEST, new JSONObject().put("error", "invalid command").toString());
							}
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
	
	private Session signIn(JSONObject data) {
		String username = data.getString("username");
		String password = data.getString("password");
		JSONObject accountData = ITAhM.getTable("account").getJSONObject();
		
		if (accountData.has(username)) {
			 JSONObject account = accountData.getJSONObject(username);
			 
			 if (account.getString("password").equals(password)) {
				// signin 성공, cookie 발행
				return Session.getInstance(account.getInt("level"));
			 }
		}
		
		return null;
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
