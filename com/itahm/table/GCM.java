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
			if (super.table.has(id)) {
				Agent.manager.gcmm.unregister(super.getJSONObject(id).getString("token"));
			}
		}
		else {
			Agent.manager.gcmm.register(gcm.getString("token"), id);
		}
		
		return super.put(id,  gcm);
	}
}
