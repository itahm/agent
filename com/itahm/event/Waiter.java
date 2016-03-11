package com.itahm.event;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import org.json.JSONObject;

import com.itahm.http.Response;

public class Waiter {

	private Response response;
	private int index;
	private Waiter next;
	
	public Waiter(Response response) {
		this(response, -1);
	}
	
	public Waiter(Response response, int i) {
		this.response = response;
		index = i;
	}

	public int index() {
		return this.index;
	}
	
	public void index(int index) {
		this.index = index;
	}
	
	public void dispatchEvent(JSONObject event) {
		if (event == null) {
			return;
		}
		
		if (this.response != null) {
			try {
				this.response.status(200, "OK").send(event.toString());
			} catch (IOException e) {
				// TODO channel 끊어줘야할것 같은데...
				e.printStackTrace();
			}
		}
	}
	
	public void next(Waiter waiter) {
		this.next = waiter;
	}
	
	public Waiter next() {
		return this.next;
	}
	
	public SocketChannel getChannel() {
		return this.response.getChannel();
	}
	
}
