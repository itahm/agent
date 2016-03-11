package com.itahm.table;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.table.Table;

public class Account extends Table {

	public Account() throws IOException {
		load("account");
		
		reset();
	}
	
	private void reset() {
		if (isEmpty()) {
			getJSONObject()
				.put("root", new JSONObject()
					.put("username", "root")
					.put("password", "root"));
		
			super.save();
		}
	}
	
	public void save(JSONObject data) {
		super.save(data);
		
		reset();
	}
}
