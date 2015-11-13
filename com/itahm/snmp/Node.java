package com.itahm.snmp;

import java.io.IOException;

public interface Node {
	public static final long TIMEOUT = 3000;
	
	public void requestCompleted(boolean success) throws IOException;
}
