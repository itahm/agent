package com.itahm;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

//import org.json.JSONObject;

import com.itahm.table.Account;
import com.itahm.table.Config;
import com.itahm.table.Critical;
import com.itahm.table.Device;
import com.itahm.table.GCM;
import com.itahm.table.Icon;
import com.itahm.table.Position;
import com.itahm.table.Profile;
import com.itahm.table.Monitor;
import com.itahm.table.Table;
import com.itahm.util.DataCleaner;

public class ITAhM {
	
	private final static String API_KEY = "AIzaSyBg6u1cj9pPfggp-rzQwvdsTGKPgna0RrA";
	public final static String VERSION = "1.3.2.4";
	public final static int MAX_TIMEOUT = 10000;
	public final static int ICMP_INTV = 1000;
	public final static int MID_TIMEOUT = 5000;
	public final static int DEF_TIMEOUT = 3000;
	private static File dataRoot;
	public static HTTPServer http;
	public static Log log;
	public static GCMManager gcmm;
	
	private static Map<String, Table> tableMap;
	
	public final static class agent {
		public static SNMPAgent snmp;
		public static ICMPAgent icmp;
	}
	
	public ITAhM(int tcp, String path, String host) throws IOException {
		//Config configTable;
		//JSONObject config;
		
		System.out.println(String.format("Version %s", VERSION));
		System.out.println("start up ITAhM agent");
		System.out.println("TCP "+ tcp);
		
		dataRoot = new File(path, "data");
		dataRoot.mkdir();
		
		tableMap = new HashMap<String, Table>();
		tableMap.put(Table.ACCOUNT, new Account());
		tableMap.put(Table.PROFILE, new Profile());
		tableMap.put(Table.DEVICE, new Device());
		tableMap.put(Table.POSITION, new Position());
		tableMap.put(Table.MONITOR, new Monitor());
		tableMap.put(Table.CONFIG, new Config());
		tableMap.put(Table.ICON, new Icon());
		tableMap.put(Table.CRITICAL, new Critical());
		tableMap.put(Table.GCM, new GCM());
		
		try {
			http = new HTTPServer("0.0.0.0", tcp);
		}
		catch (BindException be) {
			System.out.println("tcp "+ tcp + " 는 사용중입니다.");
			
			throw be;
		}
		
		
		log = new Log();
		gcmm = new GCMManager(API_KEY, host);
		
		//config = configTable.getJSONObject();
		
		try {
			agent.snmp = new SNMPAgent();
		}
		catch (BindException be) {
			System.out.println("udp " + 162 +" 는 사용중입니다.");
			
			throw be;
		}
		
		agent.icmp = new ICMPAgent();
		
		clean(new File(dataRoot, "node"));
	}
	
	public static File getRoot() {
		return dataRoot;
	}
	
	public static Table getTable(String tableName) {
		return tableMap.get(tableName);
	}
	
	public static void shutdown() {
		http.close();
		
		agent.snmp.close();
		agent.icmp.close();
		
		log.close();
		
		gcmm.close();
		
		for (Table table : tableMap.values()) {
			try {
				table.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("shut down ITAhM agent");
	}
	
	private void clean(File nodeRoot) {
		Calendar date = Calendar.getInstance();
		
		date.set(Calendar.HOUR_OF_DAY, 0);
		date.set(Calendar.MINUTE, 0);
		date.set(Calendar.SECOND, 0);
		date.set(Calendar.MILLISECOND, 0);
		
		date.add(Calendar.MONTH, -3);
				
		new DataCleaner(nodeRoot, date.getTimeInMillis(), 3) {

			@Override
			public void onDelete(File file) {
			}
			
			@Override
			public void onComplete(long count) {
			}
		};
	}
	
	public static void main(String[] args) throws IOException {
		String path = ".";
		int tcp = 2014;
		String host = InetAddress.getLocalHost().getHostAddress();
		
		for(int i=0, length = args.length; i<length;) {
			if (args[i].equals("-host")) {
				if (++i < length) {
					host = args[i++];
				}
				else {
					return;
				}
			}
			else if (args[i].equals("-path")) {
				if (++i < length) {
					path = args[i++];
				}
				else {
					return;
				}
			}
			else if (args[i].equals("-tcp")) {
				if (++i < length) {
					tcp = Integer.parseInt(args[i++]);
				}
				else {
					return;
				}
			}
			else if (args[i].equals("-help")) {
				System.out.println("");
				System.out.println("-path DIRECTORY_PATH");
				System.out.println("\tpath where data archive will be placed (current directory if omitted)");
				System.out.println("");
				System.out.println("-tcp TCP_PORT_NUMBER");
				System.out.println("\tservice port(tcp) number (2014 if omitted)");
				
				return;
			}
			else {
				System.out.println("invalid arguments. see man page with -help");
				
				return;
			}
		}
		
		try {
			new ITAhM(tcp, path, host);
		}
		catch (UnknownHostException uhe) {
			System.out.println("dns is not responding.");
		}
		catch (BindException be) {
			
		}

	}
}
