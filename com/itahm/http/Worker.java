package com.itahm.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.json.JSONObject;

import com.itahm.ITAhMException;
import com.itahm.command.Command;

public class Worker {

	private static enum Commander {
		ECHO("com.itahm.command.Echo"),
		SIGNIN("com.itahm.command.SignIn"),
		SIGNOUT("com.itahm.command.SignOut"),
		PULL("com.itahm.command.Pull"),
		PUSH("com.itahm.command.Push"),
		QUERY("com.itahm.command.Query"),
		SELECT("com.itahm.command.Select"),
		LISTEN("com.itahm.command.Listen");
		
		private String className;
		
		private Commander(String s) {
			className = s;
		}
		
		private Command getCommand() {
			try {
				return (Command)Class.forName(this.className).newInstance();
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			}
			
			return null;
		}
		
		public static Command getCommand(String command) {
			try {
				return valueOf(command.toUpperCase()).getCommand();
			}
			catch (IllegalArgumentException iae) {
				
			}
		
			return null;
		}
	};
	
	private final SocketChannel channel;
	private final Parser parser;
	
	public Worker(SocketChannel sc) {
		channel = sc;
		parser = new Parser();
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
		
		if (bytes == -1) {
			close();
		}
		else {
			buffer.flip();
			
			
			try {
				parse(buffer);
			} catch (IOException e) {
				// 모든 ioexception은 여기에서 받아야함.
				
				close();
			}
			
		}
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
			Response response = new Response(channel);
			
			response.status(400, "Bad Request").header("Connection", "Close").send();
		}
	}
	
	private void processRequest(Request request) throws IOException{
		Response response = new Response(channel);
		
		String method = request.method();
		String cookie = request.cookie();
		
		if (!"HTTP/1.1".equals(request.version())) {
			response.status(505, "HTTP Version Not Supported").send();
		}
		else {
			if ("OPTIONS".equals(method)) {
				response.status(200, "OK").header("Allow", "OPTIONS, POST, GET").send();
			}
			else if ("POST".equals(method)/* || "GET".equals(method)*/) {
				JSONObject data = request.getJSONObject();
				
				if (data == null) {
					response.status(400, "Bad Request").send();
				}
				else {
					if (cookie != null) {
						Session session = Session.find(cookie);
						if (session != null) {
							session.update();
						}
					}
					
					Command command = Commander.getCommand(data.getString("command"));
					
					command.execute(request, response);
				}
			}
			else {
				response.status(405, "Method Not Allowed").header("Allow", "OPTIONS, POST").send();
			}
		}
	}
	
	private void close() {
		try {
			this.channel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String [] args) {
	}
	
}
