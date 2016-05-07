package com.itahm.util;

public class Queue {

	private static final int QUEUE_SIZE = 1024;
	
	private final String [] queue;
	private final int capacity;
	private int position = -1;
	
	public Queue() {
		this(QUEUE_SIZE);
	}
	
	public Queue(int size) {
		capacity = size;
		queue = new String [size];
	}

	public synchronized int push(String message) {
		this.position = ++this.position % this.capacity;
		
		this.queue[this.position] = message;
		
		return this.position;
	}
	
	/**
	 * 
	 * @return 다음 position의 index
	 */
	public synchronized int next() {
		return (this.position + 1) % this.capacity;
	}
	
	public synchronized String get(int index) {
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
	public synchronized String pop() {
		return get(this.position);		
	}
	
}
