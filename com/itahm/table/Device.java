package com.itahm.table;

import java.io.IOException;

import org.json.JSONObject;

public class Device extends Table {

	public Device() throws IOException {
		load("device");
		
		if (isEmpty()) {
			getJSONObject().put("0", new JSONObject()
				.put("id", "0")
				.put("ip", "127.0.0.1")
				.put("x", 0).put("y", 0)
				.put("name", "localhost")
				.put("profile", "public")
				.put("type", "server")
			);
			
			save();
		}
	}

}
