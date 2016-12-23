package com.itahm;

import java.net.UnknownHostException;

import com.itahm.icmp.AbstractNode;
import com.itahm.icmp.ICMPListener;

public class ICMPNode extends AbstractNode {

	private static final int [] TIMEOUT = new int [] {ITAhM.DEF_TIMEOUT, ITAhM.MID_TIMEOUT, ITAhM.MAX_TIMEOUT};
	private static final int MAX_RETRY = 3; 
	
	private final ICMPListener listener;
	private final String host;
	private int failure = 0;
	
	public ICMPNode(ICMPListener listener, String host) throws UnknownHostException {
		super(host);
		
		this.host = host;
		this.listener = listener;
	}
	
	@Override
	public void onSuccess(long time) {
		if (failure > 0) {
			failure = 0;
			
			super.setTimeout(TIMEOUT[failure]);
		}
		
		listener.onSuccess(this.host, time);
	}
	
	@Override
	public void onFailure() {
		failure++;
		
		if (failure < MAX_RETRY) {
			super.setTimeout(TIMEOUT[failure]);
		}
		else {
			failure--;
			
			this.listener.onFailure(host);
		}
	}
	
	public static void main(String[] args) throws UnknownHostException {
		ICMPNode node = new ICMPNode(new ICMPListener() {

			@Override
			public void onSuccess(String host, long time) {
				System.out.println(host + " time="+ time);
			}

			@Override
			public void onFailure(String host) {
				System.out.println(host + " timeout");
			}
			
		}, args[0]);
		
		node.start();
		
		try {
			Thread.sleep(100000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		node.stop();
	}
	
}
