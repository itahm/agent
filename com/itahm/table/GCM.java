package com.itahm.table;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.table.Table;

public class GCM extends Table {

	public GCM() throws IOException {
		super.load(GCM);
	}
	
	@Override
	public JSONObject put(String id, JSONObject gcm) {
		if (gcm == null) {
			gcm = super.remove(id);
			
			ITAhM.gcmm.unregister(gcm.getString("token"));
		}
		else {
			super.put(id,  gcm);
			
			ITAhM.gcmm.register(gcm.getString("token"), id);
		}
		
		return super.table;
	}
}
