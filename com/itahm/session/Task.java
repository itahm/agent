package com.itahm.session;

import java.util.TimerTask;

public class Task extends TimerTask {

	Session session;
	
	public Task(Session session) {
		this.session = session;
	}
	
	@Override
	public void run() {
		Session.remove(this.session);
	}
	
}