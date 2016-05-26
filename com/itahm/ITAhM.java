package com.itahm;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import com.itahm.table.Account;
import com.itahm.table.Device;
import com.itahm.table.Icon;
import com.itahm.table.Profile;
import com.itahm.table.Table;

public class ITAhM extends Timer {
	
	private final static String API_KEY = "AIzaSyBg6u1cj9pPfggp-rzQwvdsTGKPgna0RrA";
	
	private static File dataRoot;
	private static HTTPServer http;
	
	public static SNMPAgent snmp;
	public static GCMManager gcmm;
	
	private static Map<String, Table> tableMap;
	
	public ITAhM(int tcp, String path, String host) throws IOException {
		super(true);
		
		System.out.println("ITAhM version 1.1.3.13");
		System.out.println("start up ITAhM agent");
		
		dataRoot = new File(path, "data");
		dataRoot.mkdir();
		
		gcmm = new GCMManager(API_KEY, host);
		
		tableMap = new HashMap<String, Table>();
		tableMap.put("account", new Account());
		tableMap.put("profile", new Profile());
		tableMap.put("device", new Device());
		tableMap.put("icon", new Icon());
		
		http = new HTTPServer("0.0.0.0", tcp);
		
		snmp = new SNMPAgent();
	}
	
	public static File getRoot() {
		return dataRoot;
	}
	
	public static void debug(String msg) {
		System.out.println(msg);
	}
	
	public static Table getTable(String tableName) {
		return tableMap.get(tableName);
	}
	
	public static void shutdown() {
		http.close();
		
		snmp.close();
		
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
			if (args[i].equals("-path")) {
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
			System.out.println("tcp "+ tcp + " is already used.");
		}

	}
}
