package com.itahm.event;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import com.itahm.http.Response;

public class Waiter {

	private Response response;
	private final int index;
	private Waiter next;
	
	public Waiter(Response response, int index) {
		this.response = response;
		this.index = index;
	}

	public void set(Response response) {
		this.response = response;
	}
	
	public int index() {
		return this.index;
	}
	
	public void checkout(Event event) throws IOException {
		if (this.response != null) {
			this.response.status(200, "OK").send(event.toString());
		}
	}
	
	public void next(Waiter waiter) {
		this.next = waiter;
	}
	
	public Waiter next() {
		return this.next;
	}
	
	public boolean own(SocketChannel channel) {
		return this.response.own(channel);
	}
}
