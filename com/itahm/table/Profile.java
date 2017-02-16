package com.itahm.table;

import java.io.File;
import java.io.IOException;

import com.itahm.Agent;
import com.itahm.json.JSONObject;

public class Profile extends Table {
	
	public Profile(File dataRoot) throws IOException {
		super(dataRoot, PROFILE);
	}
	
	private void removeProfile(JSONObject profile) {
		if ("v3".equals(profile.getString("version"))) {
			Agent.manager.snmp.removeUSM(profile.getString("user"));
		}
	}
	
	public JSONObject put(String name, JSONObject profile) {
		boolean success = true;
		
		// 삭제
		if (profile == null) {
			if (super.table.has(name) && Agent.manager.snmp.isIdleProfile(name)) {
				removeProfile(super.table.getJSONObject(name));
			}
			else {
				success = false;
			}
		}
		// 변경은 불가
		else if (super.table.has(name)) {
			success = false;
		}
		// v3 추가
		else if ("v3".equals(profile.getString("version"))) {
			if (profile.has("authentication")) {
				if (profile.has("privacy")) {
					success = Agent.manager.snmp.addUSM(profile.getString("user"), profile.getString("authentication"), profile.getString("privacy"));
				}
				else {
					success = Agent.manager.snmp.addUSM(profile.getString("user"), profile.getString("authentication"));
				}
			}
			else {
				success = Agent.manager.snmp.addUSM(profile.getString("user"));
			}
		}
		// else v2c 추가
		
		return success? super.put(name, profile): super.table;
	}
}
