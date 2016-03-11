package com.itahm;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import org.json.JSONObject;

import com.itahm.event.Queue;
import com.itahm.event.Waiter;

public class Event {

	private final static String STRING_DATA = "data";
	
	private final Queue eventQueue = new Queue();
	private Waiter waiter; // null
	
	public Event() {
	}
	
	/**
	 * waiter가 원하는 이벤트 있으면 돌려주고 없으면 waiter 큐에 추가  
	 * @param waiter
	 * @throws IOException 
	 */
	public synchronized void listen(Waiter waiter) throws IOException {
		int index = waiter.index();
		JSONObject event = this.eventQueue.get(index);
		
		if (event == null) {
			waiter.index(eventQueue.next());
			
			if (this.waiter != null) {
				waiter.next(this.waiter);
			}
			
			this.waiter = waiter;
		}
		else {
			waiter.dispatchEvent(event);
		}
	}
	
	public synchronized void cancel(SocketChannel channel) {
		Waiter tmp = null;
		Waiter waiter = this.waiter;
		
		while (waiter != null) {
			if (waiter.getChannel() == channel) {
				// waiter 목록에서 제거
				if (tmp == null) {
					this.waiter = waiter.next();
				}
				else {
					tmp.next(waiter.next());
				}
			}
			else {
				tmp = waiter;
				waiter = waiter.next();
			}
		}
	}
	
	public synchronized void dispatch(JSONObject data) {
		JSONObject event = this.eventQueue.push(new JSONObject().put(STRING_DATA, data));
		
		while (this.waiter != null) {
			
			this.waiter.dispatchEvent(event);
		
			this.waiter = this.waiter.next();
		}
	}
	
}
