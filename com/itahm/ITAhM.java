package com.itahm;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.nio.channels.SocketChannel;
//import java.util.HashMap;
//import java.util.Map;
import java.util.Timer;

//import org.json.JSONObject;

//import com.itahm.http.Listener;
import com.itahm.http.Listener;
import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.table.Table;
//import com.itahm.command.Command;

public class ITAhM extends Timer implements EventListener, Closeable {
	
	private static File dataRoot;
	private static DataBase data;
	private static SnmpManager snmp;
	private final Listener http;
	public static final Event event = new Event();
	
	public ITAhM(int tcp) throws IOException, ITAhMException {
		this(tcp, ".");
	}
	public ITAhM(int tcp, String path) throws IOException {
		super(true);
		
		System.out.println("ITAhM service is started");

		// 초기화 순서 중요함.
		
		dataRoot = new File(path, "data");
		dataRoot.mkdir();
				
		data = new DataBase();
		
		http = new Listener(tcp);
		
		scheduleAtFixedRate(snmp = new SnmpManager(), 0, 30000);
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

	@Override
	public void close() {
		try {
			this.http.close();
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
		
		System.out.println("ITAhM service is end");
	}
	
	public static void main(String [] args) throws IOException, ITAhMException {
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
			else {
				return;
			}
		}
		try {
			final ITAhM itahm = new ITAhM(tcp, path);
			
			Runtime.getRuntime().addShutdownHook(new Thread()
	        {
	            @Override
	            public void run()
	            {
	            	itahm.close();
	            }
	        });
		}
		catch (BindException be) {
			System.out.println("tcp "+ tcp + " is already used.");
		}
	}

	@Override
	public void onConnect(SocketChannel channel) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onRequest(Request request, Response response) {
		// TODO Auto-generated method stub
	}
	
}
