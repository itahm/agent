package com.itahm.http;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


public abstract class Listener implements Runnable, Closeable {

	private final ServerSocketChannel channel;
	private final ServerSocket listener;
	private final Selector selector;
	private final ByteBuffer buffer;
	private final Set<SocketChannel> connections = new HashSet<SocketChannel>();
	
	private boolean closed;
	
	public Listener() throws IOException {
		this("0.0.0.0", 80);
	}

	public Listener(String ip) throws IOException {
		this(ip, 80);
	}
	
	public Listener(int tcp) throws IOException {
		this("0.0.0.0", tcp);
	}
	
	public Listener(String ip, int tcp) throws IOException {
		this(new InetSocketAddress(InetAddress.getByName(ip), tcp));
	}
	
	public Listener(InetSocketAddress addr) throws IOException {
		channel = ServerSocketChannel.open();
		listener = channel.socket();
		selector = Selector.open();
		buffer = ByteBuffer.allocateDirect(1024);
		
		listener.bind(addr);
		channel.configureBlocking(false);
		channel.register(selector, SelectionKey.OP_ACCEPT);
		
		new Thread(this).start();
		
		onStart();
	}
	
	private void onConnect() {
		SocketChannel channel = null;
		
		try {
			channel = this.channel.accept();
			
			channel.configureBlocking(false);
			channel.register(this.selector, SelectionKey.OP_READ, new Request(channel, this));
			
			// 이것 제대로 지워주지 않으면 메모리 릭!
			this.connections.add(channel);
			
			return;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (channel != null) {
			try {
				channel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void onRead(SelectionKey key) {
		SocketChannel channel = (SocketChannel)key.channel();
		Request request = (Request)key.attachment();
		int bytes ;
		
		this.buffer.clear();
		
		try {
			bytes = channel.read(buffer);
			
			if (bytes != -1) {
				if (bytes > 0) {
					buffer.flip();
					
					request.parse(this.buffer);
				}
				
				return;
			}
		} catch (IOException ioe) {
			// RESET에 의한 예외일 수 있음.
		}
		
		onClose(request, true);
		
		disconnect(channel);
	}

	protected void disconnect(SocketChannel channel) {
		try {
			channel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		this.connections.remove(channel);
	}
	
	@Override
	public synchronized void close() throws IOException {
		if (this.closed) {
			return;
		}
		
		for (SocketChannel channel : this.connections) {
			channel.close();
		}
		
		this.closed = true;
			
		this.selector.wakeup();
	}

	@Override
	public void run() {
		Iterator<SelectionKey> iterator = null;
		SelectionKey key = null;
		int count;
		
		while(!this.closed) {
			try {
				count = this.selector.select();
			} catch (IOException e) {
				e.printStackTrace();
				
				continue;
			}
			
			if (count > 0) {
				iterator = this.selector.selectedKeys().iterator();
				while(iterator.hasNext()) {
					key = iterator.next();
					iterator.remove();
					
					if (!key.isValid()) {
						continue;
					}
					
					if (key.isAcceptable()) {
						onConnect();
					}
					else if (key.isReadable()) {
						onRead(key);
					}
				}
			}
		}
		
		try {
			this.selector.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			this.listener.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		onStop();
	}
	
	abstract protected void onRequest(Request request);
	abstract protected void onClose(Request request, boolean closed);
	abstract protected void onStart();
	abstract protected void onStop();
	
	public static void main(String [] args) throws IOException {
		final Listener server = new Listener() {

			@Override
			protected void onRequest(Request request) {
				
				request.getRequestURI();
				request.getRequestMethod();
				
				try {
					
					request.sendResponse(Response.getInstance(200, "OK",
						"<!DOCTYPE html><html><head><title>test</title></head></html>")
							.setResponseHeader("Connection", "Close"));

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			@Override
			protected void onClose(Request request, boolean closed) {
				// TODO Auto-generated method stub
				
			}

			@Override
			protected void onStart() {
				System.out.println("HTTP Server running...");
			}

			@Override
			protected void onStop() {
				System.out.println("stop HTTP Server.");
			}
			
		};
		
		System.in.read();
		
		server.close();
	}
	
}
