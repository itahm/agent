package com.itahm.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.itahm.ITAhMException;

// TODO: Auto-generated Javadoc
/**
 * The Class Parser.
 */
public class Parser {
	
	/**
	 * The Enum Status.
	 */
	private static enum Status {
		
		/** The init. */
		init,
		
		/** The header. */
		header,
		
		/** The body. */
		body,
		
		/** The closed. */
		closed
	}
	
	/** The message. */
	private Request message;
	
	/** The buffer. */
	private byte [] buffer;
	
	/** The content length. */
	private int contentLength = -1;
	
	/** The body. */
	private final ByteArrayOutputStream body;
	
	/** The status. */
	private Status status;
	
	/**
	 * Instantiates a new parser.
	 */
	public Parser() {
		message = new Request();
		body = new ByteArrayOutputStream();
		status = Status.init;
	}
	
	/**
	 * Update.
	 *
	 * @param src the src
	 * @return true, if successful
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws ITAhMException 
	 */
	public boolean update(ByteBuffer src) throws IOException, ITAhMException {
		try {
			switch(this.status) {
			case init:
				return parseRequest(src);
				
			case header:
				return parseHeader(src);
				
			case body:
				return parseBody(src);
				
			default:
				return false;
			}
		}
		catch (ITAhMException itahme) {
			this.status = Status.closed;
			
			throw itahme;
		}
	}
	
	/**
	 * Message.
	 *
	 * @return the message
	 */
	public Request message() {
		return this.message;
	}
	
	/**
	 * Body.
	 *
	 * @return the JSON object
	 *
	public JSONObject body() {
		byte [] body = this.body.toByteArray();
		
		try {
			return body.length > 0? new JSONObject(this.decoder.decode(ByteBuffer.wrap(body)).toString()): null;
		} catch (JSONException | CharacterCodingException e) {
			return null;
		}
	}
	*/
	
	/**
	 * client가 socket이 닫히지 않은 상태로 재요청 하게되면 parser도 재활용됨.
	 */
	public void clear() {
		this.body.reset();
		this.message = new Request();
	}
	
	/**
	 * Parses the request.
	 *
	 * @param src the src
	 * @return true, if successful
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws ITAhMException 
	 */
	private boolean parseRequest(ByteBuffer src) throws IOException, ITAhMException {
		String line = readLine(src);
		
		if (line == null) {
			// line을 얻지 못함. Listener의 다음 call을 기대함
			// 사실 발생하면 안되는...
			return false;
		}
		
		if (line.length() == 0) {
			//규약에 의해 request-line 이전의 빈 라인은 무시한다.
			return parseRequest(src);
		}
		
		// request-line 파싱
		if (!this.message.request(line)) {
			throw new ITAhMException("invalid request line");
		}
		
		this.status = Status.header;
		
		return parseHeader(src);
	}
	
	/**
	 * Parses the header.
	 *
	 * @param src the src
	 * @return true, if successful
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws ITAhMException 
	 */
	private boolean parseHeader(ByteBuffer src) throws IOException, ITAhMException {
		String line = readLine(src);
		
		if (line == null) {
			// line을 얻지 못함. Listener의 다음 call을 기대함
			
			return false;
		}
		
		// header-field 파싱
		if (line.length() > 0) {
			int index = line.indexOf(":");
			
			if (index == -1) {
				throw new ITAhMException("invalid header field");
			}
			
			this.message.header(line.substring(0, index), line.substring(index + 1).trim());
			
			return parseHeader(src);
		}
		
		// header 파싱 완료
		this.contentLength = this.message.length();
		
		if (this.contentLength < 0) {
			throw new ITAhMException("invalid content-length");
		}
		
		src.compact().flip();
		
		this.status = Status.body;
		
		return parseBody(src);
	}
	
	/**
	 * Parses the body.
	 *
	 * @param src the src
	 * @return true, if successful
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws ITAhMException 
	 */
	private boolean parseBody(ByteBuffer src) throws IOException, ITAhMException {
		byte [] bytes = new byte[src.limit()];
		
		src.get(bytes);
		this.body.write(bytes);
		
		int length = this.body.size();
		
		if (length < this.contentLength) {
			return false;
		}
		
		if (length > this.contentLength) {
			throw new ITAhMException(String.format("out of content length %d/%d", length, this.contentLength));
		}
		
		// body 조합 완료
		this.message.body(this.body.toByteArray());
		this.status = Status.init;
		
		return true;
	}
	
	/**
	 * Read line.
	 *
	 * @param src the src
	 * @return the string
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private String readLine(ByteBuffer src) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		
		if (this.buffer != null) {
			buffer.write(this.buffer);
			
			this.buffer = null;
		}
		
		int b;
		
		while(src.hasRemaining()) {
			b = src.get();
			buffer.write(b);
			
			if (b == Message.LF) {
				String line = readLine(buffer.toByteArray());
				if (line != null) {
					return line;
				}
			}
		}
		
		this.buffer = buffer.toByteArray();
		
		return null;
	}
	
	/**
	 * Read line.
	 *
	 * @param src the src
	 * @return the string
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private String readLine(byte [] src) throws IOException {
		int length = src.length;
		
		if (length > 1 && src[length - 2] == Message.CR) {
			return new String(src, 0, length -2);
		}
		
		return null;
	}
	
}
