package com.itahm.table;

import java.io.IOException;

import org.json.JSONObject;

import com.itahm.ITAhM;

public class Profile extends Table {
	
	public final static String COMMUNITY = "community";
	public final static String NAME = "name";
	public final static String VERSION = "version";
	public final static String UDP = "udp";
	public final static String PUBLIC = "public";
	
	public Profile() throws IOException {
		load("profile");
		
		if (isEmpty()) {
			getJSONObject().put(PUBLIC, new JSONObject()
				.put(NAME, "public")
				.put(COMMUNITY, "public")
				.put(VERSION, "v2c")
				.put(UDP,  161));
			
			super.save();
		}
		
	}	

	public JSONObject save(JSONObject data) {
		super.save(data);
		
		ITAhM.snmp.reload();
		
		return data;
	}
	
}
