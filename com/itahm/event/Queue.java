package com.itahm.event;

import org.json.JSONObject;

public class Queue {

	private static final int QUEUE_SIZE = 1024;
	
	private final JSONObject [] queue;
	private final int capacity;
	private int position = -1;
	
	public Queue() {
		this(QUEUE_SIZE);
	}
	
	public Queue(int size) {
		capacity = size;
		queue = new JSONObject [size];
	}

	public synchronized JSONObject push(JSONObject event) {
		this.position = ++this.position % this.capacity;
		
		event.put("index", this.position);
		
		this.queue[this.position] = event;
		
		return event;
	}
	
	/**
	 * 
	 * @return 다음 position의 index
	 */
	public synchronized int next() {
		return (this.position + 1) % this.capacity;
	}
	
	public synchronized JSONObject get(int index) {
		try {
			return this.queue[index];
		}
		catch (ArrayIndexOutOfBoundsException aioobe) {
			return null;
		}
	}
	
	/**
	 * 
	 * @return 가장 마지막 작성된 event
	 */
	public synchronized JSONObject get() {
		return get(this.position);		
	}
	
}
