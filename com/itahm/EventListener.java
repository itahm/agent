package com.itahm;

import java.nio.channels.SocketChannel;

import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.event.Event;

public interface EventListener {
	public void onConnect(SocketChannel channel);
	public void onClose(SocketChannel channel);
	public void onRequest(Request request, Response response);
	public void onError(Exception e);
	public void onEvent(Event event);
}
