package com.itahm;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.util.Timer;

import com.itahm.http.Listener;
import com.itahm.table.Table;

public class ITAhM extends Timer {
	
	private final static long REQUEST_INTERVAL = 10000;
	
	private static File dataRoot;
	private static DataBase data;
	private static SnmpManager snmp;
	private static Listener http;
	
	public ITAhM(int tcp, String path) throws IOException {
		super(true);
		
		System.out.println("start up ITAhM agent");

		// 초기화 순서 중요함.
		
		dataRoot = new File(path, "data");
		dataRoot.mkdir();
				
		data = new DataBase();
		
		http = new Listener(tcp);
		
		scheduleAtFixedRate(snmp = new SnmpManager(), 0, REQUEST_INTERVAL);
	}
	
	public static File getRoot() {
		return dataRoot;
	}
	
	public static SnmpManager getSnmp() {
		return snmp;
	}
	
	public static Table getTable(String tableName) {
		return data.getTable(tableName);
	}
	
	public static void shutdown() {
		try {
			http.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			snmp.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			data.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("shut down ITAhM agent");
	}
	
	public static void main(String[] args) throws IOException {
		String path = ".";
		int tcp = 2014;
		
		
		for(int i=0, length = args.length; i<length;) {
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
			new ITAhM(tcp, path);
		}
		catch (BindException be) {
			System.out.println("tcp "+ tcp + " is already used.");
		}

	}
}
