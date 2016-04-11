package com.itahm.http;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Session {

	private static final Map<String, Session> sessions = new ConcurrentHashMap<String, Session>();
	private static final Timer timer = new Timer(true);
	
	static long timeout = 60 * 30 * 1000;
	//static long timeout = 60 * 1000;
	
	private final String id;
	private final int level;
	private TimerTask task;
	
	private Session(String uuid, int lvl) {
		id = uuid;
		level = lvl;
		
		update();
	}

	public static void remove(Session session) {
		sessions.remove(session.getID());
	}
	
	public static Session getInstance(int level) {
		String uuid = UUID.randomUUID().toString();
		Session session = new Session(uuid, level);
		
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
	
	public int getLevel() {
		return this.level;
	}
	
	public void update() {
		final String id = this.id;
		
		if (this.task != null) {
			this.task.cancel();
		}
		
		this.task = new TimerTask() {

			@Override
			public void run() {
				sessions.remove(id);
			}
		};
		
		timer.schedule(this.task, timeout);
	}
	
	public void close() {
		this.task.cancel();
		
		remove(this);
	}
	
	public static void main(String [] args) {
	}
	
}
