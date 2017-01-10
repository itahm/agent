package com.itahm.command;

public enum Commander {
	PULL("com.itahm.command.Pull"),
	PUSH("com.itahm.command.Push"),
	PUT("com.itahm.command.Put"),
	QUERY("com.itahm.command.Query"),
	SELECT("com.itahm.command.Select"),
	LINK("com.itahm.command.Link"),
	LISTEN("com.itahm.command.Listen"),
	SHUTDOWN("com.itahm.command.ShutDown"),
	MESSAGE("com.itahm.command.Message"),
	TOP("com.itahm.command.Top"),
	LOG("com.itahm.command.Log"),
	HISTORY("com.itahm.command.History"),
	SYSLOG("com.itahm.command.Syslog"),
	ARP("com.itahm.command.ARP"),
	CONFIG("com.itahm.command.Config"),
	SEARCH("com.itahm.command.Search"),
	EXTRA("com.itahm.command.Extra"),
	NETWORK("com.itahm.command.Network");
	
	private String className;
	
	private Commander(String s) {
		className = s;
	}
	
	private Command getCommand() {
		try {
			return (Command)Class.forName(this.className).newInstance();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
		}
		
		return null;
	}
	
	public static Command getCommand(String command) {
		try {
			return valueOf(command.toUpperCase()).getCommand();
		}
		catch (IllegalArgumentException iae) {
		}
	
		return null;
	}
}
