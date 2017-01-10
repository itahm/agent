package com.itahm.table;

import java.io.File;
import java.io.IOException;

import com.itahm.json.JSONObject;

public class Position extends Table {
	
	public Position(File dataRoot) throws IOException {
		super(dataRoot, POSITION);
		
		if (isEmpty()) {
			getJSONObject().put("127.0.0.1", new JSONObject()
				.put("x", 0)
				.put("y", 0)
			);
			
			save();
		}
	}

}
