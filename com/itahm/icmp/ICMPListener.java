package com.itahm.icmp;

public interface ICMPListener {
	public void onSuccess(String host);
	public void onFailure(String host);
}
