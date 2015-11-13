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

	public int assign() throws IOException {
		int index;
		
		JSONObject table = getJSONObject();
		
		synchronized(table) {
			index = table.getInt("index");
			table.put("index", index +1);
			
			save();
		}
		
		return index;
	}
}
