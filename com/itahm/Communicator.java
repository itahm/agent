package com.itahm;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.nio.charset.StandardCharsets;

import com.itahm.json.JSONException;
import com.itahm.json.JSONObject;

import com.itahm.http.Listener;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class Communicator extends Listener {
	
	private final static String DATA = "data";
	
	private final File root;
	private ITAhMAgent agent;
	
	public Communicator(int tcp) throws Exception {
		super("0.0.0.0", tcp);
		
		System.out.println(String.format("ITAhM communicator started with TCP %d.", tcp));
		
		root = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
		
		loadAgent();
	}
	
	@Override
	protected void onStart() {
		System.out.println("HTTP Server start.");
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
		this.agent.closeRequest(request);
	}
	
	
	
	private void loadAgent() throws Exception {
		File dataRoot = new File(root, DATA);
		dataRoot.mkdir();
		
		this.agent = new com.itahm.Agent();
		agent.start(dataRoot);
	}
	
	private void processRequest(Request request) throws IOException{
		Response response = parseRequest(request);
		
		if (response != null) { /* listen인 경우 null*/
			request.sendResponse(response);
		}
	}
	private Response parseRequest(Request request) throws IOException{
		if (!"HTTP/1.1".equals(request.getRequestVersion())) {
			return Response.getInstance(Response.Status.VERSIONNOTSUP);
		}
		
		switch(request.getRequestMethod()) {
		case "OPTIONS":
			return Response.getInstance(Response.Status.OK).setResponseHeader("Allow", "OPTIONS, GET, POST");
		
		case"POST":
			JSONObject data;
			
			try {
				data = new JSONObject(new String(request.getRequestBody(), StandardCharsets.UTF_8.name()));
				
				if (!data.has("command")) {
					return Response.getInstance(Response.Status.BADREQUEST, new JSONObject().put("error", "command not found").toString());
				}
				
				switch (data.getString("command")) {
				case "agent":
					return Response.getInstance(Response.Status.OK,
						new JSONObject()
							.put("connections", getConnectionSize())
							.put("space", this.root.getUsableSpace())
							.put("java", System.getProperty("java.version")).toString());
				}
				
			} catch (JSONException e) {
				return Response.getInstance(Response.Status.BADREQUEST, new JSONObject().put("error", "invalid json request").toString());
			} catch (UnsupportedEncodingException e) {
				return Response.getInstance(Response.Status.BADREQUEST, new JSONObject().put("error", "UTF-8 encoding required").toString());
			}
			
			return this.agent.executeRequest(request, data);
			
		case "GET":
			Response response = null;
			File uri = new File(this.root, request.getRequestURI());
			
			if (uri.isFile()) {
				response = Response.getInstance(Response.Status.OK, uri);
			}
			
			if (response != null) {
				return response;
			}
				
			return Response.getInstance(Response.Status.NOTFOUND);
			
		}
		
		return Response.getInstance(Response.Status.NOTALLOWED).setResponseHeader("Allow", "OPTIONS, POST, GET");
	}
	
	public void close() {
		try {
			super.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws IOException {
		int tcp = 2014;
		
		if (args.length > 0) {
			try {
				tcp = Integer.parseInt(args[0]);
			}
			catch(NumberFormatException nfe) {}
		}
		
		try {
			final Communicator c = new Communicator(tcp);
			
			Runtime.getRuntime().addShutdownHook(
				new Thread() {
					public void run() {
						c.close();
					}
				});
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
