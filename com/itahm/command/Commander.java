package com.itahm.command;

public enum Commander {
	ECHO("com.itahm.command.Echo"),
	SIGNIN("com.itahm.command.SignIn"),
	SIGNOUT("com.itahm.command.SignOut"),
	PULL("com.itahm.command.Pull"),
	PUSH("com.itahm.command.Push"),
	PUT("com.itahm.command.Put"),
	QUERY("com.itahm.command.Query"),
	SELECT("com.itahm.command.Select"),
	LINK("com.itahm.command.Link"),
	LISTEN("com.itahm.command.Listen"),
	SHUTDOWN("com.itahm.command.ShutDown"),
	REGISTER("com.itahm.command.Register"),
	UNREGISTER("com.itahm.command.UnRegister"),
	MESSAGE("com.itahm.command.Message"),
	TOP("com.itahm.command.Top"),
	LOG("com.itahm.command.Log"),
	AGENT("com.itahm.command.Agent");
	
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
