package com.itahm.json;

import java.io.File;
import java.io.IOException;

import com.itahm.json.JSONObject;

public class JSONSummary extends Data {
	
	public JSONSummary(File f) {
		super(f);
	}

	@Override
	public void buildNext(File dir) {
		File file = new File(dir, "summary");
		
		if (file.isFile()) {
			try {	
				JSONObject data = JSONFile.getJSONObject(file);
				
				for (Object key : data.keySet()) {
					super.data.put((String)key, data.getJSONObject((String)key));
				}
			} catch (IOException e) {}
		}
	}	
}
