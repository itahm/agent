package com.itahm.table;

import java.io.IOException;

import com.itahm.table.Table;

public class Config extends Table {

	public Config() throws IOException {
		load(CONFIG);
		
		if (isEmpty()) {
			getJSONObject()
				.put("timeout", 5000);
			
			save();
		}
	}
}
