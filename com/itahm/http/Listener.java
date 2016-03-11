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

import com.itahm.ITAhM;

public class Listener implements Runnable, Closeable {

	private final ServerSocketChannel channel;
	private final ServerSocket listener;
	private final Selector selector;
	private final Thread thread;
	private boolean shutdown;
	private final ByteBuffer buffer;
	
	public Listener() throws IOException {
		this(80);
	}

	public Listener(int tcp) throws IOException {
		channel = ServerSocketChannel.open();
		listener = channel.socket();
		selector = Selector.open();
		thread = new Thread(this);
		shutdown = false;
		buffer = ByteBuffer.allocateDirect(1024);
		
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
						onRead((Response)key.attachment());
					}
				}
			}
		}
		
		System.out.println("shutdown");
	}
	
	private void onConnect(SocketChannel channel) throws IOException {
		channel.configureBlocking(false);
		channel.register(selector, SelectionKey.OP_READ, new Response(channel));
	}
	
	private void onRead(Response response) throws IOException {
		this.buffer.clear();
		
		// buffer를 재활용하는것이 성능에 좋다는 판단에 인자로 넘겨줌
		// 추후 확인할것.
		if (!response.update(this.buffer)) {
			SocketChannel channel = response.getChannel();
			
			ITAhM.event.cancel(channel);
			
			channel.close();
		}
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
			ioe.printStackTrace();
		}
	}
	
}
