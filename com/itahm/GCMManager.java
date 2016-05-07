package com.itahm;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONObject;

import com.itahm.gcm.DownStream;
import com.itahm.json.JSONFile;

public class GCMManager extends DownStream implements Closeable {

	public static final String ID = "id";
	public static final String TOKEN = "token";
	
	/**
	 * token - id
	 */
	private final Map<String, String> tokenMapping = new HashMap<String, String>();
	/**
	 * id - token
	 */
	private final Map<String, String> idMapping = new HashMap<String, String>();
	private JSONFile file;
	
	public GCMManager(String apiKey, String host) throws IOException {
		super(apiKey, host);
		
		file = new JSONFile(new File(ITAhM.getRoot(), "gcm"));
		JSONObject jsono = file.getJSONObject();
		String token;
		
		if (jsono.length() > 0) {
			for(String id : JSONObject.getNames(jsono)) {
				token = jsono.getString(id);
				
				idMapping.put(id, token);
				tokenMapping.put(token, id);
			}
		}
	}

	public void broadcast(String message) {
		Iterator<String> it;
	
		synchronized(this.tokenMapping) {
			it = this.tokenMapping.keySet().iterator();
			
			while(it.hasNext()) {
				try {
					send(it.next(), "ITAhM message", message);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void save() {
		JSONObject jsono = new JSONObject();
		Iterator<String> it;
		String id;
		
		synchronized(this.idMapping) {
			it = this.idMapping.keySet().iterator();
			while (it.hasNext()) {
				id = it.next();
				
				jsono.put(id, this.idMapping.get(id));
			}
		}
		
		try {
			this.file.save(jsono);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void register(String id, String token) {
		synchronized (this.idMapping) {
			this.idMapping.put(id, token);
		}
			
		synchronized (this.tokenMapping) {
			this.tokenMapping.put(token, id);
		}
		
		save();
	}
	
	@Override
	public void onUnRegister(String token) {
		String id;
		
		synchronized (this.tokenMapping) {
			id = this.tokenMapping.remove(token);
		}
		
		if (id == null) {
			System.out.println("unregister failed: no such token\n"+ token);
		}
		else {
			synchronized (this.tokenMapping) {
				this.idMapping.remove(id);
			}
			
			save();
		}
	}

	@Override
	public void onRefresh(String old, String token) {
		String id = this.tokenMapping.remove(token);
		
		if (id != null) {
			this.idMapping.put(id, token);
		}
		else {
			System.out.println("refresh failed: no shuch Token");
		}
	}

	@Override
	public void onComplete(int status) {
		if (status != 200) {
			System.out.println("gcm failure");
		}
	}

	@Override
	public void onStart() {
		System.out.println("GCM DownStream ON.");
	}

	@Override
	public void onStop() {
		System.out.println("GCM DownStream OFF.");
	}
	
	@Override
	public void close() {
		try {
			this.file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		super.close();
	}
}
