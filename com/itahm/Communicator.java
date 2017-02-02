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
	
	enum Options {
		PATH, TCP, CLEAN;
	}
	
	private File root;
	private ITAhMAgent agent;
	
	private Communicator(int tcp) throws Exception {
		super("0.0.0.0", tcp);
		
		System.out.println(String.format("ITAhM communicator started with TCP %d.", tcp));
	}
	
	public Communicator(int tcp, File path, boolean clean) throws Exception {
		this(tcp);
		
		if (path == null) {
			path = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
		}
		
		loadAgent(path, clean);
	}
	
	public Communicator(File path, boolean clean) throws Exception {
		this(2014, path, clean);
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
	
	private void loadAgent(File root, boolean clean) throws Exception {
		File dataRoot = new File(root, DATA);
		dataRoot.mkdir();
		
		this.root = root;
		
		this.agent = new Agent();
		agent.start(dataRoot, clean);
	}
	
	private void processRequest(Request request) throws IOException{
		Response response = parseRequest(request);
		
		if (response != null) { /* listen인 경우 null*/
			request.sendResponse(response);
		}
	}
	private Response parseRequest(Request request) throws IOException{
		if (!"HTTP/1.1".equals(request.getRequestVersion())) {
			return Response.getInstance(request, Response.Status.VERSIONNOTSUP);
		}
		
		switch(request.getRequestMethod()) {
		case "OPTIONS":
			return Response.getInstance(request, Response.Status.OK).setResponseHeader("Allow", "OPTIONS, GET, POST");
		
		case"POST":
			JSONObject data;
			
			try {
				data = new JSONObject(new String(request.getRequestBody(), StandardCharsets.UTF_8.name()));
				
				if (!data.has("command")) {
					return Response.getInstance(request, Response.Status.BADREQUEST, new JSONObject().put("error", "command not found").toString());
				}
				
				switch (data.getString("command")) {
				case "agent":
					return Response.getInstance(request, Response.Status.OK,
						new JSONObject()
							.put("connections", getConnectionSize())
							.put("space", this.root.getUsableSpace())
							.put("java", System.getProperty("java.version")).toString());
				}
				
			} catch (JSONException e) {
				return Response.getInstance(request, Response.Status.BADREQUEST, new JSONObject().put("error", "invalid json request").toString());
			} catch (UnsupportedEncodingException e) {
				return Response.getInstance(request, Response.Status.BADREQUEST, new JSONObject().put("error", "UTF-8 encoding required").toString());
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
		File path = null;
		boolean clean = true;
		
		for (int i=0, _i=args.length; i<_i; i++) {
			if (args[i].indexOf("-") != 0) {
				System.out.println("잘못된 옵션 형식이 입력되어 실행을 중단합니다.");
				
				return;
			}
			
			try {
				switch(Options.valueOf(args[i].substring(1).toUpperCase())) {
				case PATH:
					path = new File(args[++i]);
					
					if (!path.isDirectory()) {
						System.out.println("PATH 옵션에 존재하지 않는 경로가 입력되어 실행을 중단합니다.");
						
						return;
					}
					
					break;
				case TCP:
					try {
						tcp = Integer.parseInt(args[++i]);
					}
					catch(NumberFormatException nfe) {
						System.out.println("TCP 옵션에 숫자가 아닌 값이 입력되어 실행을 중단합니다.");
						
						return;
					}
					
					break;
				case CLEAN:
					try {
						clean = Boolean.valueOf(args[++i]);
					}
					catch(IllegalArgumentException iae) {
						System.out.println("CLEAN 옵션에 잘못된 값이 입력되어 실행을 중단합니다.");
						
						return;
					}
					
					break;
				}
			}
			catch(IllegalArgumentException iae) {
				break;
			}
		}
		
		try {
		
			final Communicator c = new Communicator(tcp, path, clean);
			
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
