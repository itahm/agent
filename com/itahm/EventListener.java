package com.itahm;

import java.nio.channels.SocketChannel;

import com.itahm.http.Request;
import com.itahm.http.Response;

public interface EventListener {
	public void onConnect(SocketChannel channel);
	public void onRequest(Request request, Response response);
}
