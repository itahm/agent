package com.itahm.command;

import java.io.IOException;

import org.json.JSONObject;

//import com.itahm.event.Event;
//import com.itahm.event.Waiter;
import com.itahm.http.Response;

public class Listen extends Command {
	
	public Listen() {
	}

	@Override
	protected void execute(JSONObject data, Response response) throws IOException {
		/*Waiter waiter = new Waiter(response, json.getInt("index"));
		int index = waiter.index();
		Event event = this.eventQueue.getNext(index);
		
		waiter.set(response);
		
		if (index == -1 || event == null) {
			this.waitingQueue.push(waiter);
			
		}
		else {
			waiter.checkout(event);
		}
		*/
		response.status(200, "OK").send();
	}

}
