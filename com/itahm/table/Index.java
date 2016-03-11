package com.itahm.table;

import java.io.IOException;

import org.json.JSONObject;

public class Index extends Table {

	public Index() throws IOException {
		load("index");
		
		if (isEmpty()) {
			getJSONObject().put("index", 1);
			
			save();
		}
	}

	public long assign() throws IOException {
		long index;
		
		JSONObject table = getJSONObject();
		
		synchronized(table) {
			index = table.getLong("index");
			table.put("index", index +1);
			
			save();
		}
		
		return index;
	}
}
