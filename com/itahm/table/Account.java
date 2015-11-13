package com.itahm.table;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.table.Table;

public class Account extends Table {

	public Account() throws IOException {
		load("account");
		
		if (isEmpty()) {
			getJSONObject().put("root", new JSONObject().put("username", "root").put("password", "root"));
			save();
		}
	}
}
