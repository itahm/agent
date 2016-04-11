package com.itahm.table;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.Constant;
import com.itahm.table.Table;

public class Account extends Table {

	public Account() throws IOException {
		load("account");
		
		reset();
	}
	
	private void reset() {
		if (isEmpty()) {
			getJSONObject()
				.put(Constant.STRING_ROOT, new JSONObject()
					.put(Constant.STRING_USER, Constant.STRING_ROOT)
					.put(Constant.STRING_PWORD, Constant.STRING_ROOT)
					.put(Constant.STRING_LEVEL, 0));
		
			super.save();
		}
	}
	
	public void save(JSONObject data) {
		super.save(data);
		
		reset();
	}
}
