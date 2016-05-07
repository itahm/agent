package com.itahm;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONObject;

import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.util.Queue;

public class Event {

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
		String message = eventQueue.get(index);
		
		if (message == null) {
			waiter.put(request, eventQueue.next());
		}
		else {
			request.sendResponse(Response.getInstance(200, "OK"
				, new JSONObject()
					.toString()));
		}
	}
	
	public static synchronized void cancel(Request request) {
		waiter.remove(request);
	}
	
	public static synchronized void dispatch(String ip, boolean status, String message) {
		Iterator<Request> it = waiter.keySet().iterator();		
		int index = eventQueue.push(message);
		JSONObject event = new JSONObject()
			.put("message", message)
			.put("ip", ip)
			.put("status", status)
			.put("index", index);
		// TODO 지금은 ip와 status 정보까지 주지만 향후 message와 index만 주는 것으로 수정할것
		while (it.hasNext()) {
			try {
				it.next().sendResponse(Response.getInstance(200, "OK", event.toString()));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			it.remove();
		}
		
		ITAhM.gcmm.broadcast(message);
	}
	
}
