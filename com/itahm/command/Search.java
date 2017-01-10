package com.itahm.command;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;

import com.itahm.json.JSONObject;

import com.itahm.Agent;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class Search implements Command {
	
	@Override
	public Response execute(Request request, JSONObject data) throws IOException {
		try {			
			com.itahm.util.Network network = new com.itahm.util.Network(InetAddress.getByName(data.getString("network")).getAddress(), data.getInt("mask"));
			Iterator<String> it = network.iterator();
			
			while(it.hasNext()) {
				Agent.manager.snmp.testNode(it.next(), false);
			}
			
			return Response.getInstance(Response.Status.OK);
		}
		catch (NullPointerException npe) {
			return Response.getInstance(Response.Status.UNAVAILABLE);
		}
	}
	
}
