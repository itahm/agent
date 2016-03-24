package com.itahm.event;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONObject;

import com.itahm.http.Response;

public class Event {

	private final static String STRING_DATA = "data";
	
	private final static Map<Response, Integer> waiter = new HashMap<Response, Integer> ();
	private final static Queue eventQueue = new Queue();
	
	public Event() {
	}
	
	/**
	 * waiter가 원하는 이벤트 있으면 돌려주고 없으면 waiter 큐에 추가  
	 * @param waiter
	 * @throws IOException 
	 */
	public static synchronized void listen(Response response, int index) throws IOException {
		JSONObject event = eventQueue.get(index);
		
		if (event == null) {
			waiter.put(response, eventQueue.next());
		}
		else {
			response.dispatchEvent(event);
		}
	}
	
	public static synchronized void cancel(Response response) {
		waiter.remove(response);
	}
	
	public static synchronized void dispatch(JSONObject data) {
		JSONObject event = eventQueue.push(new JSONObject().put(STRING_DATA, data));
		Iterator<Response> it = waiter.keySet().iterator();		
		
		while (it.hasNext()) {
			it.next().dispatchEvent(event);
			
			it.remove();
		}
	}
	
}
