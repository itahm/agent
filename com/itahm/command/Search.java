package com.itahm.command;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;

import org.json.JSONObject;

import com.itahm.ITAhM;
import com.itahm.http.Request;
import com.itahm.http.Response;

public class Search implements Command {
	
	@Override
	public Response execute(Request request, JSONObject data) throws IOException {
		try {			
			com.itahm.util.Network network = new com.itahm.util.Network(InetAddress.getByName(data.getString("network")).getAddress(), data.getInt("mask"));
			Iterator<String> it = network.iterator();
			
			while(it.hasNext()) {
				ITAhM.agent.snmp.testNode(it.next(), false);
			}
			
			return Response.getInstance(Response.Status.OK);
		}
		catch (NullPointerException npe) {
			return Response.getInstance(Response.Status.UNAVAILABLE);
		}
	}
	
}
