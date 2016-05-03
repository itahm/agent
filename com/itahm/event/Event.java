package com.itahm.event;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class Event {

	private final static String STRING_DATA = "data";
	
	private final static Map<Request, Integer> waiter = new HashMap<Request, Integer> ();
	private final static Queue eventQueue = new Queue();
	
	public Event() {
	}
	
	/**
	 * waiter가 원하는 이벤트 있으면 돌려주고 없으면 waiter 큐에 추가  
	 * @param waiter
	 * @throws IOException 
	 */
	public static synchronized void listen(Request request, int index) throws IOException {
		JSONObject event = eventQueue.get(index);
		
		if (event == null) {
			waiter.put(request, eventQueue.next());
		}
		else {
			request.sendResponse(Response.getInstance(200, "OK", event.toString()));
		}
	}
	
	public static synchronized void cancel(Request request) {
		waiter.remove(request);
	}
	
	public static synchronized void dispatch(JSONObject data) {
		JSONObject event = eventQueue.push(new JSONObject().put(STRING_DATA, data));
		Iterator<Request> it = waiter.keySet().iterator();		
		
		while (it.hasNext()) {
			try {
				it.next().sendResponse(Response.getInstance(200, "OK", event.toString()));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			it.remove();
		}
		
		ITAhM.gcmm.broadcast(data.toString());
	}
	
}
