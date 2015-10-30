package com.itahm.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public final class Response extends Message {

	public final static String COOKIE = "SESSION=%s; HttpOnly";
	private final SocketChannel channel;
	
	public Response(SocketChannel sock) {
		channel = sock;
	}
	
	public Response status(int status, String reason) {
		this.startLine = "HTTP/1.1 "+ status +" "+ reason +CRLF;
		
		return this;
	}
	
	public Response header(String fieldName, String fieldValue) {
		this.header.put(fieldName, fieldValue);
		
		return this;
	}
	
	public void body(byte [] body) {
		int length = body.length;
		
		this.body = new byte[length];
		
		System.arraycopy(body, 0, this.body, 0, length);
	}
	
	public boolean own(SocketChannel channel) {
		return channel == this.channel;
	}
	/*
	public byte [] build() throws IOException {
		Iterator<String> iterator = this.header.keySet().iterator();
		String key;
		StringBuilder header = new StringBuilder();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();		
		
		header.append(startLine +CRLF);
		header.append(String.format(FIELD, "Access-Control-Allow-Headers", "Authorization, Content-Type"));
		header.append(String.format(FIELD, "Access-Control-Allow-Origin", "http://app.itahm.com"));
		header.append(String.format(FIELD, "Access-Control-Allow-Origin", "http://free.itahm.com"));
		header.append(String.format(FIELD, "Access-Control-Allow-Credentials", "true"));
		
		while(iterator.hasNext()) {
			key = iterator.next();
			
			header.append(String.format(FIELD, key, this.header.get(key)));
		}
		
		header.append(CRLF);
		baos.write(header.toString().getBytes("US-ASCII"));
		baos.write(this.body);
		
		return baos.toByteArray();
	}
	*/
	/**
	 * body 없는 전송
	 * @throws IOException
	 */
	public void send() throws IOException {
		sendHeader(0);
	}
	
	/**
	 * 문자열 전송시 사용
	 * @param channel
	 * @param body
	 * @throws IOException
	 */
	public void send(String body) throws IOException {
		byte [] bytes = body.getBytes("UTF-8");
		
		sendHeader(bytes.length);
		send(ByteBuffer.wrap(bytes));
	}
	
	/**
	 * file 전송시 사용
	 */
	public void send(File body) throws IOException {
		try (
			FileInputStream fis = new FileInputStream(body);
		) {
			long size = body.length();
			byte [] buffer;
			
			sendHeader(size);
			
			while (size > 0) {
				buffer = new byte [(int)size];
				size -= (int)size;
				fis.read(buffer);
				
				send(ByteBuffer.wrap(buffer));
			}
		}
	}
	
	/**
	 * 이미 완성된 header와 전송할 data를 조합하여 전송
	 */
	private void sendHeader(long length) throws IOException {
		Iterator<String> iterator;
		StringBuilder header = new StringBuilder();
		String key;
		
		header.append(this.startLine);
		header.append(String.format(FIELD, "Access-Control-Allow-Headers", "Authorization, Content-Type"));
		//header.append(String.format(FIELD, "Access-Control-Allow-Origin", "http://app.itahm.com"));
		header.append(String.format(FIELD, "Access-Control-Allow-Origin", "http://localhost"));
		header.append(String.format(FIELD, "Access-Control-Allow-Credentials", "true"));
		header.append(String.format(FIELD, "Content-Length", Long.toString(length)));
		
		iterator = this.header.keySet().iterator();
		
		while(iterator.hasNext()) {
			key = iterator.next();
			
			header.append(String.format(FIELD, key, this.header.get(key)));
		}
		
		header.append(CRLF);
		
		send(ByteBuffer.wrap(header.toString().getBytes("US-ASCII")));
	}
	
	/**
	 * 최종적으로 socket channel에 ByteBuffer 형식으로 data를 전송하는 method 
	 * @param channel
	 * @param buffer
	 * @return 전송한 data size (bytes)
	 * @throws IOException
	 */
	private int send(ByteBuffer buffer) throws IOException {
		int bytes = 0;
		
		while (buffer.remaining() > 0) {
			bytes += this.channel.write(buffer);
		}
		
		return bytes;
	}
	
}