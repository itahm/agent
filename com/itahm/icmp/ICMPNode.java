package com.itahm.icmp; 

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ICMPNode implements Runnable {

	private final ICMPListener listener;
	private long interval = 10000;
	private int timeout;
	private boolean pending = false;
	
	private final InetAddress target;
	private final String host;
	private Thread thread;

	public ICMPNode(ICMPListener listener, String host, int timeout) throws UnknownHostException {
		this.listener = listener;
		this.host = host;
		this.timeout = timeout;
		
		target = InetAddress.getByName(host);	
	}
	
	public void setInterval(long interval) {
		this.interval = interval;
	}
	
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
	public synchronized boolean start() {
		if (this.thread != null) {
			return false;
		}
		
		this.thread = new Thread(this);
		this.thread.start();
		
		return true;
	}
	
	public synchronized void stop() {
		this.thread.interrupt();
		
		this.thread = null;
	}
	
	@Override
	public void run() {
		while(!this.thread.isInterrupted()) {
			try {
				if (this.target.isReachable(this.timeout)) {
					this.pending = false;
					
					listener.onSuccess(this.host);
				}
				else {
					if (this.pending) {
						this.pending = false;
						
						listener.onFailure(this.host);
					}
					else {
						this.pending = true;
					}
					
					continue;
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			
			try {
				Thread.sleep(this.interval);
			} catch (InterruptedException ie) {
				break;
			}
		}
	}

	public static void main(String[] args) throws UnknownHostException {
		ICMPNode node = new ICMPNode(new ICMPListener() {

			@Override
			public void onSuccess(String host) {
				// TODO Auto-generated method stub
				System.out.println("O");
			}

			@Override
			public void onFailure(String host) {
				// TODO Auto-generated method stub
				System.out.println("X");
			}
			
		}, "192.168.0.1", 5000);
		
		node.start();
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		node.stop();
		
	}
	
}
