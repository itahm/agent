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
import java.util.Iterator;
import java.util.Set;

import com.itahm.EventListener;
import com.itahm.event.Event;

public class Listener implements Runnable, Closeable {

	private final ServerSocketChannel channel;
	private final ServerSocket listener;
	private final Selector selector;
	private final Thread thread;
	private boolean shutdown;
	private final ByteBuffer buffer;
	private final EventListener itahm;
	
	public Listener(EventListener itahm) throws IOException {
		this(itahm , 80);
	}

	public Listener(EventListener handler, int tcp) throws IOException {
		channel = ServerSocketChannel.open();
		listener = channel.socket();
		selector = Selector.open();
		thread = new Thread(this);
		shutdown = false;
		buffer = ByteBuffer.allocateDirect(1024);
		itahm = handler;
		
		listener.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), tcp));
		channel.configureBlocking(false);
		channel.register(selector, SelectionKey.OP_ACCEPT);
		thread.start();
	}
	
	private void listen() throws IOException{
		Set<SelectionKey> selectedKeys = null;
		Iterator<SelectionKey> iterator = null;
		SelectionKey key = null;
		int count;
		
		while(!this.shutdown) {
			count = selector.select();
			
			if (count > 0) {
				selectedKeys = selector.selectedKeys();
	
				iterator = selectedKeys.iterator();
				while(iterator.hasNext()) {
					key = iterator.next();
					iterator.remove();
					
					if (key.isAcceptable()) {
						onConnect(channel.accept());
					}
					else if (key.isReadable()) {
						onRead((Worker)key.attachment());
					}
				}
			}
		}
		
		System.out.println("shutdown");
	}
	
	private void onConnect(SocketChannel channel) throws IOException {
		channel.configureBlocking(false);
		channel.register(selector, SelectionKey.OP_READ, new Worker(channel));
	}
	
	private void onRead(Worker session) throws IOException {
		this.buffer.clear();
		
		// buffer를 재활용하는것이 성능에 좋다는 판단에 인자로 넘겨줌
		// 추후 확인할것.
		session.update(this.buffer);
	}

	public void onClose(SocketChannel channel) throws IOException {
		this.itahm.onClose(channel);
		
		channel.close();
	}
	
	@Override
	public void close() throws IOException {
		if (this.shutdown) {
			return;
		}
		
		this.shutdown = true;
			
		this.selector.wakeup();
		
		try {
			this.thread.join();
		} catch (InterruptedException ie) {
		}
	}

	@Override
	public void run() {
		try {
			listen();
			
			this.shutdown = true;
			
			this.selector.close();
			this.listener.close();
		}
		catch(IOException ioe) {
			this.itahm.onError(ioe);
		}
	}

	public static void main(String[] args) throws IOException {
		final Listener listener = new Listener(new EventListener() {
			@Override
			public void onConnect(SocketChannel channel) {
				try {
					System.out.println("connect >> "+ channel.getRemoteAddress());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			@Override
			public void onClose(SocketChannel channel) {
				try {
					System.out.println("close >> "+ channel.getRemoteAddress());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			@Override
			public void onRequest(Request request, Response response) {
				
			}

			@Override
			public void onError(Exception e) {
				e.printStackTrace();
			}

			@Override
			public void onEvent(Event event) {
				// TODO Auto-generated method stub
				
			}
			
		}, 2015);

		Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
            	try {
					listener.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        }
        });
	}
	
}
