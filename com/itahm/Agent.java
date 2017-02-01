package com.itahm;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import com.itahm.json.JSONException;
import com.itahm.json.JSONObject;

import com.itahm.command.Command;
import com.itahm.command.Commander;
import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.http.Session;
import com.itahm.table.Account;
import com.itahm.table.Config;
import com.itahm.table.Critical;
import com.itahm.table.Device;
import com.itahm.table.GCM;
import com.itahm.table.Icon;
import com.itahm.table.Monitor;
import com.itahm.table.Position;
import com.itahm.table.Profile;
import com.itahm.table.Table;

public class Agent implements ITAhMAgent {

	private final static String API_KEY = "AIzaSyBg6u1cj9pPfggp-rzQwvdsTGKPgna0RrA";
	public final static String VERSION = "1.3.3.8";
	public final static int MAX_TIMEOUT = 10000;
	public final static int ICMP_INTV = 1000;
	public final static int MID_TIMEOUT = 5000;
	public final static int DEF_TIMEOUT = 3000;
	
	private static Map<String, Table> tableMap = new HashMap<>();
	
	public final static class manager {
		public static Log log;
		public static GCMManager gcmm;
		public static SNMPAgent snmp;
		public static ICMPAgent icmp;
	}
	
	public Agent() {
		System.out.println(String.format("ITAhM Agent version %s.1 ready.", VERSION));
	}
	
	public boolean start(File dataRoot, boolean clean) {		
		try {
			tableMap.put(Table.ACCOUNT, new Account(dataRoot));
			tableMap.put(Table.PROFILE, new Profile(dataRoot));
			tableMap.put(Table.DEVICE, new Device(dataRoot));
			tableMap.put(Table.POSITION, new Position(dataRoot));
			tableMap.put(Table.MONITOR, new Monitor(dataRoot));
			tableMap.put(Table.CONFIG, new Config(dataRoot));
			tableMap.put(Table.ICON, new Icon(dataRoot));
			tableMap.put(Table.CRITICAL, new Critical(dataRoot));
			tableMap.put(Table.GCM, new GCM(dataRoot));
			
			manager.gcmm = new GCMManager(API_KEY, InetAddress.getLocalHost().getHostAddress());
			manager.log = new Log(dataRoot);
			manager.snmp = new SNMPAgent(dataRoot, clean);
			manager.icmp = new ICMPAgent();
			
			System.out.println("ITAhM agent up.");
			
			return true;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		stop();
		
		return false;
	}
	
	private Session signIn(JSONObject data) {
		String username = data.getString("username");
		String password = data.getString("password");
		JSONObject accountData = getTable(Table.ACCOUNT).getJSONObject();
		
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
	
	public static Table getTable(String table) {
		return tableMap.get(table);
	}
	
	@Override
	public Response executeRequest(Request request, JSONObject data) {
		String cmd = data.getString("command");
		Session session = getSession(request);
		
		if ("signin".equals(cmd)) {
			if (session == null) {
				try {
					session = signIn(data);
				} catch (JSONException jsone) {
					jsone.printStackTrace();
					
					return Response.getInstance(Response.Status.BADREQUEST, new JSONObject().put("error", "invalid json request").toString());
				}
			}
			
			if (session == null) {
				return Response.getInstance(Response.Status.UNAUTHORIZED);
			}
			
			return Response.getInstance(Response.Status.OK,	new JSONObject()
				.put("level", (int)session.getExtras())
				.put("version", VERSION).toString())
					.setResponseHeader("Set-Cookie", String.format("SESSION=%s; HttpOnly", session.getCookie()));
		}
			
		if ("signout".equals(cmd)) {
			if (session != null) {
				session.close();
			}
			
			return Response.getInstance(Response.Status.OK);
		}
		
		Command command = Commander.getCommand(cmd);
		
		if (command == null) {
			return Response.getInstance(Response.Status.BADREQUEST, new JSONObject().put("error", "invalid command").toString());
		}
		
		try {
			if ("put".equals(cmd) && "gcm".equals(data.getString("database"))) {
				return command.execute(request, data);
			}
			
			if (session != null) {
				return command.execute(request, data);
			}
		}
		catch (IOException ioe) {
			return Response.getInstance(Response.Status.UNAVAILABLE, new JSONObject().put("error", ioe).toString());
		}
			
		return Response.getInstance(Response.Status.UNAUTHORIZED);
	}

	@Override
	public void closeRequest(Request request) {
		manager.log.cancel(request);
	}

	@Override
	public void stop() {
		if (manager.snmp != null) {
			manager.snmp.close();
		}
		
		if (manager.icmp != null) {
			manager.icmp.close();
		}
		
		if (manager.log != null) {
			manager.log.close();
		}
		
		if (manager.gcmm != null) {
			manager.gcmm.close();
		}
		
		for (Table table : tableMap.values()) {
			try {
				table.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("ITAhM agent down.");
	}

	@Override
	public void get(String key) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void set(Object value) {
		// TODO Auto-generated method stub
		
	}

	public static void main(String [] args) {
		
	}

}
