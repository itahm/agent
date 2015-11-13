package com.itahm.table;

import java.io.IOException;

import org.json.JSONObject;

public class Profile extends Table {

	public Profile() throws IOException {
		load("profile");
		
		if (isEmpty()) {
			getJSONObject().put("public", new JSONObject()
				.put("name", "public")
				.put("version", "v2c")
				.put("community", "public")
				.put("udp", 161)
			);
			
			save();
		}
	}
}
