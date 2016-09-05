package com.itahm;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import com.itahm.table.Account;
import com.itahm.table.Critical;
import com.itahm.table.Device;
import com.itahm.table.Icon;
import com.itahm.table.Position;
import com.itahm.table.Profile;
import com.itahm.table.Snmp;
import com.itahm.table.Table;

public class ITAhM {
	
	private final static String API_KEY = "AIzaSyBg6u1cj9pPfggp-rzQwvdsTGKPgna0RrA";
	
	private static File dataRoot;
	public static HTTPServer http;
	public static Log log;
	public static SNMPAgent agent;
	public static GCMManager gcmm;
	
	private static Map<String, Table> tableMap;
	
	public ITAhM(int tcp, String path, String host) throws IOException {
		System.out.println("ITAhM version 1.1.3.32");
		System.out.println("start up ITAhM agent");
		
		dataRoot = new File(path, "data");
		dataRoot.mkdir();
		
		log = new Log(dataRoot);
		
		gcmm = new GCMManager(API_KEY, host);
		
		tableMap = new HashMap<String, Table>();
		tableMap.put(Table.ACCOUNT, new Account());
		tableMap.put(Table.PROFILE, new Profile());
		tableMap.put(Table.DEVICE, new Device());
		tableMap.put(Table.POSITION, new Position());
		tableMap.put(Table.SNMP, new Snmp());
		tableMap.put(Table.ICON, new Icon());
		tableMap.put(Table.CRITICAL, new Critical());
		
		agent = new SNMPAgent();
		
		http = new HTTPServer("0.0.0.0", tcp);
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
		
		agent.close();
		
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
