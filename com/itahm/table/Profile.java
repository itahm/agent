package com.itahm.table;

import java.io.IOException;

import org.json.JSONObject;

public class Profile extends Table {
	
	public Profile() throws IOException {
		load(PROFILE);
		
		if (isEmpty()) {
			getJSONObject().put("public", new JSONObject()
				.put("name", "public")
				.put("community", "public")
				.put("version", "v2c")
				.put("udp",  161));
			
			super.save();
		}
		
	}
	
}
