package com.itahm.http;

import java.util.HashMap;
import java.util.Map;

public class Message {

	public final static String CRLF = "\r\n";
	public final static byte CR = (byte)'\r';
	public final static byte LF = (byte)'\n';
	public final static String FIELD = "%s: %s"+ CRLF;
	
	protected String startLine;
	protected final Map<String, String> header;
	protected byte [] body;
	
	public Message() {
		header = new HashMap<String, String>();
	}
	
}
