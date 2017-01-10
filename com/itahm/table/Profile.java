package com.itahm.table;

import java.io.File;
import java.io.IOException;

import com.itahm.json.JSONObject;

public class Profile extends Table {
	
	public Profile(File dataRoot) throws IOException {
		super(dataRoot, PROFILE);
		
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
