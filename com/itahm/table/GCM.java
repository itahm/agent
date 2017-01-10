package com.itahm.table;

import java.io.File;
import java.io.IOException;

import com.itahm.json.JSONObject;

import com.itahm.Agent;
import com.itahm.table.Table;

public class GCM extends Table {

	public GCM(File dataRoot) throws IOException {
		super(dataRoot, GCM);
	}
	
	@Override
	public JSONObject put(String id, JSONObject gcm) {
		if (gcm == null) {
			gcm = super.remove(id);
			
			Agent.manager.gcmm.unregister(gcm.getString("token"));
		}
		else {
			super.put(id,  gcm);
			
			Agent.manager.gcmm.register(gcm.getString("token"), id);
		}
		
		return super.table;
	}
}
