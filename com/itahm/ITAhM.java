package com.itahm;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.itahm.ITAhMAgent;
import com.itahm.http.Listener;
import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.json.JSONException;
import com.itahm.json.JSONObject;

public class ITAhM extends Listener {
	
	private final static String DATA = "data";
	
	enum Options {
		PATH, TCP;
	}
	
	private final File root;
	private ITAhMAgent agent;
	
	public ITAhM(int tcp, File root, boolean clean) throws Exception {
		super("0.0.0.0", tcp);
		
		System.out.format("ITAhM communicator started with TCP %d.\n", tcp);
		
		this.root = root == null? new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile(): root;
		
		System.out.format("Directory : %s\n", this.root.getAbsoluteFile());
		
		System.out.format("Agent loading...\n");
		
		this.agent = loadAgent();
		
		File dataRoot = new File(root, DATA);
		dataRoot.mkdir();
		
		this.agent.start(dataRoot);
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
	
	private static ITAhMAgent loadAgent() {
		return new Agent();
	}
	/*
	private ITAhMAgent loadAgent() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		File jar = new File(this.root, AGENT_JAR);
		
		if (!jar.isFile()) {
			download(new URL("http", "itahm.com", "/download/"+ AGENT_JAR), jar);
		}
		
		try (URLClassLoader ucl = new URLClassLoader(new URL [] {jar.toURI().toURL()})) {
			return (ITAhMAgent)(ucl.loadClass("com.itahm.Agent").newInstance());
		}
	}
	*/
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
	
	public static void download(URL url, File output) throws IOException {
		HttpURLConnection hurlc = (HttpURLConnection)url.openConnection();
		
		hurlc.setConnectTimeout(3000);
		
		try {
			hurlc.setRequestMethod("GET");
			hurlc.connect();
		
			if (hurlc.getResponseCode() == 200) {
				try (InputStream is = hurlc.getInputStream()) {
					try (FileOutputStream fos = new FileOutputStream(output)) {
						int length;
						
						byte [] buffer = new byte [1024];
						
						while(true) {
							length = is.read(buffer);
							
							if (length == -1) {
								break;
							}
							
							fos.write(buffer, 0, length);
						}
					}
				}	
			}
			else {
				throw new IOException("HTTP status "+ hurlc.getResponseCode());
			}
		}
		finally {
			hurlc.disconnect();
		}
	}
	
	public static void main(String[] args) {
		int tcp = 2014;
		File path = null;
		boolean clean = true;
		
		System.out.format("ITAhM Agent, since 2014.\n");
		
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
				}
			}
			catch(IllegalArgumentException iae) {
				break;
			}
		}
		
		try {
		
			//final ITAhM c = new ITAhM(tcp, path, clean);
			final ITAhM c = new ITAhM(tcp, new File("f:\\data"), clean);
			
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
