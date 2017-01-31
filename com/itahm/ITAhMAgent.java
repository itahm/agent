package com.itahm;

import java.io.File;

import com.itahm.json.JSONObject;

import com.itahm.http.Request;
import com.itahm.http.Response;

public interface ITAhMAgent {
	public Response executeRequest(Request request, JSONObject data);
	public void closeRequest(Request request);
	public boolean start(File root, boolean clean);
	public void stop();
	public void get(String key);
	public void set(Object value);
}
