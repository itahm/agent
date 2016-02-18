package com.itahm.table;

import java.io.IOException;

import org.json.JSONObject;

public class Profile extends Table {

	public Profile() throws IOException {
		load("profile");
		
		reset();
	}	

	private void reset() throws IOException {
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
	
	public void save(JSONObject data) throws IOException {
		super.save(data);
		
		reset();
	}
	
}
