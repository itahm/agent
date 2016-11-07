package com.itahm;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;

import org.json.JSONObject;
import org.productivity.java.syslog4j.server.SyslogServer;
import org.productivity.java.syslog4j.server.SyslogServerConfigIF;
import org.productivity.java.syslog4j.server.SyslogServerEventIF;
import org.productivity.java.syslog4j.server.SyslogServerIF;
import org.productivity.java.syslog4j.server.SyslogServerSessionlessEventHandlerIF;
import org.productivity.java.syslog4j.server.impl.net.udp.UDPNetSyslogServer;

import com.itahm.util.DailyFile;

public class SyslogAgent implements SyslogServerSessionlessEventHandlerIF, Closeable {

	private static final long serialVersionUID = 1065396043436438839L;

	private static final int UDP_SYSLOG = 514;
	
	private final UDPNetSyslogServer server;
	private DailyFile dailyFile;
	private final JSONObject log;
	
	public SyslogAgent() throws IOException {
		this(UDP_SYSLOG);
	}
	
	public SyslogAgent(int udp) throws IOException {
		File syslogRoot = new File(ITAhM.getRoot(), "syslog");
		SyslogServerConfigIF config;
		byte [] bytes;
		
		syslogRoot.mkdir();
		
		this.dailyFile = new DailyFile(syslogRoot);
		bytes = this.dailyFile.read(DailyFile.trim(Calendar.getInstance()).getTimeInMillis());
		
		if (bytes == null) {
			this.log = new JSONObject();
		}
		else {
			this.log = new JSONObject(new String(bytes, StandardCharsets.UTF_8.name()));
		}
		
		server = (UDPNetSyslogServer)SyslogServer.getThreadedInstance("udp");
		config = server.getConfig();
		
		config.setPort(udp);
		config.addEventHandler(this);
	}
	
	public String getLog(long mills) throws IOException {
		byte [] bytes = this.dailyFile.read(mills);
		
		if (bytes != null) {
			return new String(bytes, StandardCharsets.UTF_8.name());
		}
		
		return new JSONObject().toString();
	}
	
	@Override
	public void destroy(SyslogServerIF ssi) {
	}

	@Override
	public void initialize(SyslogServerIF ssi) {
		System.out.println("Syslog agent OK.");
	}

	@Override
	public void close() {
		this.server.shutdown();
		
		try {
			this.dailyFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void event(SyslogServerIF ssi, SocketAddress sa, SyslogServerEventIF sseif) {
		JSONObject logData = new JSONObject();
		long date = sseif.getDate().getTime();
		
		logData
			.put("date", date)
			.put("ip", ((InetSocketAddress)sa).getAddress().getHostAddress())
			.put("host", sseif.getHost())
			.put("message", sseif.getMessage())
			.put("severity", sseif.getLevel())
			.put("facility", sseif.getFacility());
		
		
		try {
			if (this.dailyFile.roll()) {
				this.log.clear();
			}
			
			this.log.put(Long.toString(date), logData);
			
			this.dailyFile.write(this.log.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8.name()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void exception(SyslogServerIF ssi, SocketAddress sa, Exception e) {
	}

}
