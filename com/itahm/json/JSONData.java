package com.itahm.json;

import java.io.File;
import java.io.IOException;

import com.itahm.json.JSONException;
import com.itahm.json.JSONObject;

public class JSONData extends Data{

	public JSONData(File f) {
		super(f);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void buildNext(File dir) {
		File [] fa = dir.listFiles();
		
		if (fa == null) {
			return;
		}
		
		JSONObject data;
		
		for (File f : fa) {
			try {
				Long.valueOf(f.getName());
				
				try {
					data = JSONFile.getJSONObject(f);
					
					for (Object key : data.keySet()) {
						super.data.put((String)key, data.getLong((String)key));
					}
				} catch (IOException | JSONException e) {
					e.printStackTrace();
				}
			}
			catch (NumberFormatException nfe) {} 
		}
	}
	
	public static void main(String [] args) {
		File root = new File(".");
		
		new JSONData(root).buildNext(new File(args[0]));
	}
}
