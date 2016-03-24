package com.itahm.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import org.json.JSONObject;

import com.itahm.Commander;
import com.itahm.ITAhMException;
import com.itahm.command.Command;
import com.itahm.event.Event;

public final class Response extends Message {

	public final static String COOKIE = "SESSION=%s; HttpOnly";
	
	private final static String STRING_COMMAND = "command";
	private final SocketChannel channel;
	private final Parser parser = new Parser();
	
	public Response(SocketChannel sc) {
		channel = sc;
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
	
	public void ok(JSONObject body) throws IOException {
		status(200, "OK").send(body.toString());
	}
	
	public void ok() throws IOException {
		status(200, "OK").send();
	}
	
	public void unauthorized() throws IOException {
		status(401, "Unauthorized").send();
	}
	
	public void badRequest(JSONObject body) throws IOException {
		status(400, "Bad request").send(body.toString());
	}
	
	public void badRequest() throws IOException {
		status(400, "Bad request").send();
	}
	
	public void dispatchEvent(JSONObject event) {
		if (event == null) {
			return;
		}
			
		try {
			status(200, "OK").send(event.toString());
		} catch (IOException e) {
			e.printStackTrace();
			
			close();
		}
	}
	
	/**
	 * 
	 * @param buffer byte buffer를 재활용하여 사용하면 성능에 좋다는 판단에 인자로 넘겨줌. 향후 판단할것. 
	 */
	public void update(ByteBuffer buffer) {
		int bytes = -1;

		// read 중에 client가 소켓을 close 하면 예외 발생
		try {
			bytes = this.channel.read(buffer);
		}
		catch (IOException ioe) {
			// bytes = -1
		}
		
		if (bytes > 0) {
			buffer.flip();
			
			try {
				parse(buffer);
				
				return;
			} catch (IOException ioe) {
				// 모든 ioexception은 여기에서 받아야함.
				ioe.printStackTrace();
			}
		}
		else if (bytes == 0) {
			return;
		}
		
		Event.cancel(this);
		
		close();
	}
	
	private void parse(ByteBuffer buffer) throws IOException {
		try {
			// parser가 request message를 완성하면
			if (parser.update(buffer)) {
				processRequest(parser.message());
				
				parser.clear();
			}
			// else continue
		}
		catch (ITAhMException itahme) {
			clear();
			
			status(400, "Bad Request").header("Connection", "Close").send();
		}
	}
	
	private void processRequest(Request request) throws IOException{
		clear();
		
		String method = request.method();
		String cookie = request.cookie();
		
		if (!"HTTP/1.1".equals(request.version())) {
			status(505, "HTTP Version Not Supported").send();
		}
		else {
			if ("OPTIONS".equals(method)) {
				status(200, "OK").header("Allow", "OPTIONS, POST, GET").send();
			}
			else if ("POST".equals(method)/* || "GET".equals(method)*/) {
				JSONObject data = request.getJSONObject();
				
				if (data == null) {
					status(400, "Bad Request").send();
				}
				else {
					if (cookie != null) {
						Session session = Session.find(cookie);
						if (session != null) {
							session.update();
						}
					}
					
					if (data.has(STRING_COMMAND)) {
						Command command = Commander.getCommand(data.getString(STRING_COMMAND));
						
						if (command != null) {
							command.execute(request, this);
							
							return;
						}
					}
					
					status(400, "Bad Request").send();
				}
			}
			else {
				status(405, "Method Not Allowed").header("Allow", "OPTIONS, POST").send();
			}
		}
	}
	/*
	public SocketChannel getChannel() {
		return this.channel;
	}
	*/
	private void close() {
		try {
			this.channel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
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
		header.append(String.format(FIELD, "Access-Control-Allow-Origin", "http://itahm.com"));
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