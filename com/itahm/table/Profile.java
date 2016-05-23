package com.itahm.table;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.ITAhM;

public class Profile extends Table {

	public Profile() throws IOException {
		load("profile");
		
		reset();
	}	

	private void reset() {
		if (isEmpty()) {
			getJSONObject()
				.put("public", new JSONObject()
					.put("name", "public")
					.put("version", "v2c")
					.put("community", "public")
					.put("udp", 161));
			
			super.save();
		}
	}
	
	public void save(JSONObject data) {
		super.save(data);
		
		ITAhM.snmp.reload();
		
		reset();
	}
	
}
