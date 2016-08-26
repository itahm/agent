package com.itahm.table;

import java.io.IOException;

import org.json.JSONObject;

public class Position extends Table {
	
	public Position() throws IOException {
		load(POSITION);
		
		if (isEmpty()) {
			getJSONObject().put("127.0.0.1", new JSONObject()
				.put("x", 0)
				.put("y", 0)
			);
			
			save();
		}
	}

}
