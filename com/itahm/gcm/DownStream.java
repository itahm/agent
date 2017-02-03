package com.itahm.gcm;
import java.net.URL;
import java.util.LinkedList;
import java.io.Closeable;
import java.io.IOException;
import java.net.HttpURLConnection;

public abstract class DownStream implements Runnable, Closeable {

	private final static String GCMURL = "http://gcm-http.googleapis.com/gcm/send";
	private final static int TIMEOUT = 10000;
	
	private final Thread thread;
	private final String host;
	private final URL url;
	private final String auth;
	private final LinkedList<Request> queue;
	
	interface Request {
		public void send() throws IOException;
	}
	
	public DownStream(String apiKey, String host) throws IOException {
		this.host = host;
		url = new URL(GCMURL);
		
		queue = new LinkedList<>();
		
		auth = "key="+ apiKey;
		
		thread = new Thread(this);
		thread.start();
	}
	
	HttpURLConnection getConnection() throws IOException {
		HttpURLConnection hurlc = (HttpURLConnection)this.url.openConnection();
		
		hurlc.setDoOutput(true);
		hurlc.setConnectTimeout(TIMEOUT);
		hurlc.setRequestMethod("POST");
		hurlc.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		hurlc.setRequestProperty("Authorization", auth);
		hurlc.connect();
		
		return hurlc;
	}	
	
	public void send(String to, String title, String body) throws IOException {
		synchronized(this.queue) {
			this.queue.add(new Message(this, to, title, body, this.host));
		}
	}
	
	@Override
	public void close() {
		this.thread.interrupt();
		
		try {
			this.thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		Request request;
	
		onStart();
		
		while (!Thread.interrupted()) {
			synchronized(this.queue) {
				request = this.queue.poll();
			}
			
			if (request == null) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ie) {
					break;
				}
			}
			else {
				try {
					request.send();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		}
		
		onStop();
	}
	
	public static void main(String[] args) throws IOException {		
	}
	
	abstract public void onUnRegister(String token);
	abstract public void onRefresh(String oldToken, String token);
	abstract public void onStart();
	abstract public void onStop();
	abstract public void onComplete(int status);
}
