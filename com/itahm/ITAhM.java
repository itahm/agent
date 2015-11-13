package com.itahm;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.itahm.http.Listener;
import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.table.Table;
import com.itahm.command.Command;
import com.itahm.event.Event;
import com.itahm.event.EventQueue;
import com.itahm.event.EventResponder;
import com.itahm.event.Waiter;
import com.itahm.event.WaitingQueue;

public class ITAhM implements EventListener, EventResponder, Closeable {
	
	private final static Map<String, String> cmdMap = new HashMap<String, String>();
	{
		cmdMap.put("echo","com.itahm.command.Echo");
		cmdMap.put("signin","com.itahm.command.SignIn");
		cmdMap.put("signout","com.itahm.command.SignOut");
		cmdMap.put("pull","com.itahm.command.Pull");
		cmdMap.put("push","com.itahm.command.Push");
		cmdMap.put("query","com.itahm.command.Query");
		cmdMap.put("select","com.itahm.command.Select");
		cmdMap.put("listen","com.itahm.command.Listen");
	}
	
	private static File dataRoot;
	private static Data data;
	private static SnmpManager snmp;
	private final Listener http;
	private final EventQueue eventQueue = new EventQueue();
	private final WaitingQueue waitingQueue = new WaitingQueue();
	
	public ITAhM(int tcp) throws IOException, ITAhMException {
		this(tcp, ".");
	}
	public ITAhM(int tcp, String path) throws IOException, ITAhMException {
		System.out.println("ITAhM service is started");

		// 초기화 순서 중요함.
		
		dataRoot = new File(path, "data");
		dataRoot.mkdir();
				
		data = new Data();
		
		snmp = new SnmpManager();
		
		http = new Listener(this, tcp);
	}
	
	private void stop() {
		
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
	
	public void processRequest(Request request, Response response) throws IOException {
		JSONObject data = request.getJSONObject();
		String cmdString = data.getString("command");
		
		try {
			String className = cmdMap.get(cmdString);
			
			if (className == null) {
				response.badRequest(new JSONObject().put("error", "invalid command"));
			}
			else {
				((Command)Class.forName(className).newInstance()).execute(request, response);
			}
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public static void postMessage(Event event) {
		
	}
	
	@Override
	public void onConnect(SocketChannel channel) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onClose(SocketChannel channel) {
		this.waitingQueue.cancel(channel);
	}
	
	@Override
	public void onRequest(Request request, Response response) {
		try {
			processRequest(request, response);
		}
		catch (IOException ioe) {
			onError(ioe);
		}
	}

	@Override
	public void onEvent(Event event) {
		this.eventQueue.push(event);
		
		this.waitingQueue.each(this);
	}
	
	@Override
	public void onError(Exception e) {
		e.printStackTrace();
		
		stop();
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

	@Override
	public void response(Waiter waiter) {
		Event event = this.eventQueue.getNext(waiter.index());
		
		if (event != null) {
			try {
				waiter.checkout(event);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
}
