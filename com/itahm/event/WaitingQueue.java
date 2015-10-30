package com.itahm.event;

import java.nio.channels.SocketChannel;

public class WaitingQueue {
	private Waiter front;
	private int count;
	
	public WaitingQueue() {
		front = null;
		count = 0;
	}
	
	public synchronized void push(Waiter waiter) {
		if (this.front == null) {
			this.front = waiter;
		}
		else {
			waiter.next(this.front);
			
			this.front = waiter;
		}
		
		this.count++;
	}
	
	
	public synchronized void each(EventResponder responder) {
		Waiter waiter = this.front;
		
		while (waiter != null) {
			responder.response(waiter);
			
			waiter = waiter.next();
		}
		
		this.front = null;
		
		this.count = 0;
	}
	
	public synchronized void cancel(SocketChannel channel) {
		Waiter prev = null;
		Waiter waiter = this.front;
		
		while (waiter != null) {
			if (waiter.own(channel)) {
				if (prev == null) {
					this.front = waiter.next();
				}
				else {
					prev.next(waiter.next());
				}
				
				this.count--;
				
				break;
			}
			else {
				prev = waiter;
				waiter = waiter.next();
			}
		}
	}
	
	public int count() {
		return this.count;
	}
}
