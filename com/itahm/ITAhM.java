package com.itahm;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.itahm.http.Listener;
import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.session.Session;
import com.itahm.command.Command;
import com.itahm.event.Event;
import com.itahm.event.EventQueue;
import com.itahm.event.EventResponder;
import com.itahm.event.Waiter;
import com.itahm.event.WaitingQueue;

public class ITAhM implements EventListener, EventResponder, Closeable {

	private final static Map<String, String> DBMap = new HashMap<String, String>();
	{
		DBMap.put("account", "com.itahm.request.Account");
		DBMap.put("device", "com.itahm.request.Device");
		DBMap.put("line", "com.itahm.request.Line");
		DBMap.put("inoctet", "com.itahm.request.InOctet");
		DBMap.put("outoctet", "com.itahm.request.OutOctet");
		DBMap.put("profile", "com.itahm.request.Profile");
		DBMap.put("address", "com.itahm.request.Address");
		DBMap.put("snmp", "com.itahm.request.Snmp");
		DBMap.put("processor", "com.itahm.request.Processor");
		DBMap.put("storage", "com.itahm.request.Storage");
		DBMap.put("memory", "com.itahm.request.Storage");
		DBMap.put("delay", "com.itahm.request.Delay");
		DBMap.put("realtime", "com.itahm.request.RealTime");
	}
	
	private final static Map<String, String> cmdMap = new HashMap<String, String>();
	{
		cmdMap.put("echo","com.itahm.command.Echo");
		cmdMap.put("pull","com.itahm.command.Pull");
		cmdMap.put("get","com.itahm.command.Get");
	}
	
	private final Listener http;
	private final SnmpManager snmp;
	private final EventQueue eventQueue;
	private final WaitingQueue waitingQueue;
	
	public ITAhM(int tcpPort, String path) throws IOException, ITAhMException {
		System.out.println("ITAhM service is started");
		
		File root = new File(path, "data");
		root.mkdir();
		
		/**
		 * Data.initialize가 가장 먼저 수행되어야함.
		 */
		Data.initialize(root);
		
		snmp = new SnmpManager(root, this);
		
		try {
			initSNMP();
		}
		catch (JSONException jsone) {
			onError(jsone);
		}
		
		http = new Listener(this, tcpPort);
		eventQueue = new EventQueue();
		waitingQueue = new WaitingQueue();
	}
	
	/**
	 * device 중 snmp가 활성화 되어있는 것들은 snmp관리 하기위해.
	 */
	private void initSNMP() {
		JSONObject deviceData = Data.getJSONObject(Data.Table.DEVICE);
		JSONObject profileData = Data.getJSONObject(Data.Table.PROFILE);
		
		String [] names = JSONObject.getNames(deviceData);
		
		if (names == null) {
			return;
		}
		
		JSONObject device;
		String profileName;
		JSONObject profile;
		
		for (int i=0, length=names.length; i<length; i++) {
			device = deviceData.getJSONObject(names[i]);
			if (!device.isNull("snmp")) {
				profileName = device.getString("snmp");
				
				if (profileData.has(profileName)) {
					profile = profileData.getJSONObject(profileName);
					
					this.snmp.addNode(device.getString("address"), profile.getInt("udp"), profile.getString("community"));
				}
			}
		}
	}
	
	private boolean signIn(String username, String password) {
		JSONObject table = Data.getJSONObject(Data.Table.ACCOUNT);
		
		try {
			if (table.has(username)) {
				JSONObject account = table.getJSONObject(username);
				
				if (account.getString("password").equals(password)) {
					return true;
				}
			}
		}
		catch (JSONException jsone) {}
		
		return false;
	}
	/*
	private void processRequest(JSONObject request) {
		String className =DBMap.get(request.getString("database"));
		
		if (className != null) {
			try {
				Class.forName(className).getDeclaredConstructor(JSONObject.class).newInstance(request);
			} catch (InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException
					| NoSuchMethodException | SecurityException
					| ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	*/
	
	private void processRequest(JSONObject request) {
		String className = DBMap.get(request.getString("database"));
		
		if (className != null) {
			try {
				Class.forName(className).getDeclaredConstructor(JSONObject.class).newInstance(request);
			} catch (InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException
					| NoSuchMethodException | SecurityException
					| ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void stop() {
		
	}
	
	public void test(Request request, Response response) throws IOException {
		JSONObject data = request.getJSONObject();
		String cmdString = data.getString("command");
		
		try {
			String className = cmdMap.get(cmdString);
			
			if (className == null) {
				response.badRequest();
			}
			else {
				((Command)Class.forName(className).newInstance()).execute(request, response);
			}
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			e.printStackTrace();
		}
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
		JSONObject json = request.getJSONObject();
		Session session = request.session();
		String command = json.getString("command");
		
		// IOException
		try {
			// JSONException
			try {
				if ("echo".equals(command) || "pull".equals(command)) {
					test(request, response);
				}
				else {
					// session 없는 요청은 signin
					if (session == null) { 
						if ("signin".equals(command) && signIn(json.getString("username"), json.getString("password"))) {
							// signin 성공, cookie 발행
							session = Session.getInstance();
							
							response.header("Set-Cookie", String.format(Response.COOKIE, session.getID())).status(200, "OK").send();
						}
						else {
							// signin 실패하거나 signin이 아닌 session 없는 요청은 거부
							response.status(401, "Unauthorized").send();
						}
					}
					// session 있는 정상 요청
					else {
						if ("signout".equals(command)) {
							session.close();
							
							response.status(401, "Unauthorized").send();
						}
						else if ("event".equals(command)) {
							Waiter waiter = new Waiter(response, json.getInt("index"));
							int index = waiter.index();
							Event event = this.eventQueue.getNext(index);
							
							waiter.set(response);
							
							if (index == -1 || event == null) {
								this.waitingQueue.push(waiter);
								
							}
							else {
								waiter.checkout(event);
							}
						}
						else if (json.has("database")) {
							processRequest(json);
							
							response.status(200, "OK").send(json.toString());
						}
						else {
							response.status(400, "Bad Request").send();
						}
					}
				}
			}
			catch(JSONException jsone) {
				response.status(400, "Bad Request").send();
			}
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
			this.snmp.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			this.http.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Data.close();
		
		System.out.println("ITAhM service is end");
	}
	
	public static void main(String [] args) throws IOException, ITAhMException {
		final ITAhM itahm = new ITAhM(2014, ".");
		
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
