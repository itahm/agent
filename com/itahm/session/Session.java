package com.itahm.session;

import java.util.Map;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Session {

	private static final Map<String, Session> sessions = new ConcurrentHashMap<String, Session>();
	private static final Timer timer = new Timer(true);
	public static long timeout = 60 * 30 * 1000;
	
	private final String id;
	private Task task;
	
	private Session(String uuid) {
		id = uuid;
		
		update();
	}

	public static void remove(Session session) {
		sessions.remove(session.getID());
	}
	
	public static Session getInstance() {
		String uuid = UUID.randomUUID().toString();
		Session session = new Session(uuid);
		
		sessions.put(uuid, session);
		
		return session;
	}
	
	public static int count() {
		return sessions.size();
	}
	
	public static Session find(String id) {
		return sessions.get(id);
	}
	
	public static void setTimeout(long newTimeout) {
		timeout = newTimeout;
	}
	
	public String getID() {
		return this.id;
	}
	
	public void update() {
		if (this.task != null) {
			this.task.cancel();
		}
		
		this.task = new Task(this);
		
		timer.schedule(task, timeout);
	}
	
	public void close() {
		if (this.task != null) {
			this.task.cancel();
			
			this.task = null;
		}
		
		sessions.remove(this.id);
	}
	
	public static void main(String [] args) {
		Session s1 = Session.getInstance();
		
		for (int i=0; i<10; i++) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			System.out.println(Session.count());
			
			s1.update();
		}
		
		
	}
}
