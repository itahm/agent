package com.itahm.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class Request {

	public final static byte CR = (byte)'\r';
	public final static byte LF = (byte)'\n';
	public final static String GET = "GET";
	public final static String POST = "POST";
	public final static String HEAD = "HEAD";
	public final static String OPTIONS = "OPTIONS";
	public final static String DELETE = "DELETE";
	public final static String TRACE = "TRACE";
	public final static String CONNECT = "CONNECT";
	
	protected Map<String, String> header;
	private final SocketChannel channel;
	private final Listener listener;
	private byte [] buffer;
	private String method;
	private String uri;
	private String version;
	private int length;
	private ByteArrayOutputStream body;
	
	public Request(SocketChannel channel, Listener listener) {
		this.channel = channel;
		this.listener = listener;
	}
	
	public void parse(ByteBuffer src) throws IOException {
		if (this.body == null) {
			String line;
			
			while ((line = readLine(src)) != null) {
				if (parseHeader(line)) {
					src.compact().flip();
					
					parseBody(src);
					
					break;
				};
			}
		}
		else {
			parseBody(src);
		}
	}
	
	public byte [] getRequestBody() {
		return this.body.toByteArray();
	}
	
	private void parseBody(ByteBuffer src) throws IOException {
		byte [] bytes = new byte[src.limit()];
		int length;
		
		src.get(bytes);
		this.body.write(bytes);
		
		length = this.body.size();
		if (this.length == length) {
			this.listener.onRequest(this);
			
			this.body = null;
			this.header = null;
		}
		else if (this.length < length){
			throw new IOException("malformed http request.");
		}
		
	}
	
	private boolean parseHeader(String line) throws IOException {
		if (this.header == null) {
			parseStartLine(line);
		}
		else {
			if ("".equals(line)) {
				try {
					String length = this.header.get("content-length");
					
					if (length == null) {
						if (POST.equals(this.method)) {
							throw new IOException("malformed http request.");
						}
						
						listener.onRequest(this);
					}
					
					this.length = Integer.parseInt(length);
				}
				catch (NumberFormatException nfe) {
					throw new IOException("malformed http request.");
				}
				
				this.body = new ByteArrayOutputStream();
				
				return true;
			}
			else {
				int index = line.indexOf(":");
				
				if (index == -1) {
					throw new IOException("malformed http request.");
				}
				
				this.header.put(line.substring(0, index).toLowerCase(), line.substring(index + 1).trim());
			}
		}
		
		return false;
	}
	
	private void parseStartLine(String line) throws IOException {
		if (line.length() == 0) {
			//규약에 의해 request-line 이전의 빈 라인은 무시한다.
			return;
		}
		
		String [] token = line.split(" ");
		if (token.length != 3) {
			throw new IOException("malformed http request.");
		}
		
		this.method = token[0];
		this.uri = token[1];
		this.version = token[2];
		
		this.header = new HashMap<String, String>();
	}
	
	private String readLine(ByteBuffer src) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		if (this.buffer != null) {
			baos.write(this.buffer);
			
			this.buffer = null;
		}
		
		int b;
		
		while(src.hasRemaining()) {
			b = src.get();
			baos.write(b);
			
			if (b == LF) {
				String line = readLine(baos.toByteArray());
				if (line != null) {
					return line;
				}
			}
		}
		
		this.buffer = baos.toByteArray();
		
		return null;
	}
	
	public static String readLine(byte [] src) throws IOException {
		int length = src.length;
		
		if (length > 1 && src[length - 2] == CR) {
			return new String(src, 0, length -2);
		}
		
		return null;
	}
	
	public String getRequestMethod() {
		return this.method;
	}
	
	public String getRequestURI() {
		return this.uri;
	}
	
	public String getRequestVersion() {
		return this.version;
	}
	
	public String getRequestHeader(String name) {
		return this.header.get(name.toLowerCase());
	}
	
	public void sendResponse(Response response) throws IOException {
		this.channel.write(response.build());
	}
	
}
