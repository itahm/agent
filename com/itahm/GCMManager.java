package com.itahm;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.itahm.json.JSONException;
import com.itahm.json.JSONObject;

import com.itahm.gcm.DownStream;
import com.itahm.table.Table;

public class GCMManager extends DownStream {

	private final Map<String, String> index = new HashMap<String, String> ();
	private final Table gcmTable;
	
	public GCMManager(String apiKey, String host) throws IOException {
		super(apiKey, host);
		
		gcmTable = Agent.getTable(Table.GCM);
		
		JSONObject gcmData = gcmTable.getJSONObject();
		String id;
		
		for (Object key : gcmData.keySet()) {
			id = (String)key;
			
			try {
				register(gcmData.getJSONObject(id).getString("token"), id);
			} catch (JSONException jsone) {
				jsone.printStackTrace();
			}
		}
	}

	public void broadcast(String message) {
		JSONObject gcmData = gcmTable.getJSONObject();
		
		for (Object id : gcmData.keySet()) {
			try {
				super.send(gcmData.getJSONObject((String)id).getString("token"), "ITAhM message", message);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			} catch (JSONException jsone) {
				jsone.printStackTrace();
			}
		}
	}

	public void register(String token, String id) {
		this.index.put(token, id);
	}
	
	public void unregister(String token) {
		this.index.remove(token);
	}
	
	@Override
	public void onUnRegister(String token) {
		JSONObject gcmData = this.gcmTable.getJSONObject();
		
		gcmData.remove(this.index.get(token));
		
		this.gcmTable.save();
	}

	@Override
	public void onRefresh(String oldToken, String token) {
		JSONObject gcmData = this.gcmTable.getJSONObject();
		String id = this.index.get(oldToken);
		
		if (gcmData.has(id)) {
			JSONObject gcm = gcmData.getJSONObject(id);
			
			gcm.put("token", token);
		}
		
		this.gcmTable.save();
	}

	@Override
	public void onComplete(int status) {
		if (status != 200) {
			System.out.println("GCM failed.");
		}
	}

	@Override
	public void onStart() {
		System.out.println("GCM manager start.");
	}

	@Override
	public void onStop() {
		System.out.println("GCM manager stop.");
	}
	
}
