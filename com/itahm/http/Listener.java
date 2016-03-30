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

import com.itahm.event.Event;

public class Listener implements Runnable, Closeable {

	private final ServerSocketChannel channel;
	private final ServerSocket listener;
	private final Selector selector;
	private final Thread thread;
	private boolean closed;
	private final ByteBuffer buffer;
	
	public Listener() throws IOException {
		this(80);
	}

	public Listener(int tcp) throws IOException {
		channel = ServerSocketChannel.open();
		listener = channel.socket();
		selector = Selector.open();
		thread = new Thread(this);
		buffer = ByteBuffer.allocateDirect(1024);
		
		listener.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), tcp));
		channel.configureBlocking(false);
		channel.register(selector, SelectionKey.OP_ACCEPT);
		thread.start();
	}
	
	private void onConnect() {
		try {
			SocketChannel channel = this.channel.accept();
			
			channel.configureBlocking(false);
			channel.register(this.selector, SelectionKey.OP_READ, new Response(channel));
			
			return;
		} catch (IOException e) {
			// client 문제로 이렇게 될 수도 있는거지만
		}
	
		try {
			//아직 아무것도 안했으므로 channel만 close
			channel.close();
		} catch (IOException e) {
			// 이건 server 문제이므로 발생하면 안됨.
			e.printStackTrace();
		}
	}
	
	private void onRead(Response response) {
		this.buffer.clear();
		
		// buffer를 재활용하는것이 성능에 좋다는 판단에 인자로 넘겨줌
		// 추후 확인할것.
		try {
			if (response.update(this.buffer)) {
				return;
			}
		} catch (IOException e) {
		}
		
		// 예외이거너 update실패 (client가 종료) 시
		Event.cancel(response);
		
		response.close();
	}

	@Override
	public synchronized void close() throws IOException {
		if (this.closed) {
			return;
		}
		
		this.closed = true;
			
		this.selector.wakeup();
	}

	@Override
	public void run() {
		Set<SelectionKey> selectedKeys = null;
		Iterator<SelectionKey> iterator = null;
		SelectionKey key = null;
		int count;
		
		System.out.println("http server running...");
		
		while(!this.closed) {
			try {
				count = this.selector.select();
			} catch (IOException e) {
				// 이건 발생하면 안될것 같은데...
				e.printStackTrace();
				
				continue;
			}
			
			if (count > 0) {
				selectedKeys = this.selector.selectedKeys();
	
				iterator = selectedKeys.iterator();
				while(iterator.hasNext()) {
					key = iterator.next();
					iterator.remove();
					
					if (key.isAcceptable()) {
						onConnect();
					}
					else if (key.isReadable()) {
						onRead((Response)key.attachment());
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
		
		System.out.println("shut down http server");
	}
	
}
