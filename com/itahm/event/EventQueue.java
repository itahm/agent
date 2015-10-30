package com.itahm.event;


public class EventQueue {

	private final Event [] queue;
	private final int capacity;
	private int position;
	
	public EventQueue() {
		this(1024);
	}
	
	public EventQueue(int size) {
		capacity = size;
		queue = new Event [size];
		position = 0;
	}

	public void push(Event event) {
		if (++this.position == this.capacity) {
			this.position = 0;
		}
		
		event.index(this.position);
		
		this.queue[this.position] = event;
	}
	
	public Event getNext(int index) {
		if (index == this.position) {
			return null;
		}
		
		if (index < 0) {
			index = this.position;
		}
		else {
			index = (index +1) % this.capacity;
		}
		
		return this.queue[index];
	}
	
}
