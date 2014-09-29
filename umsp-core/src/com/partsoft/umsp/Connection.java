package com.partsoft.umsp;

import java.io.IOException;

public interface Connection {
	
	void handle() throws IOException;

	boolean isIdle();
	
	String getProtocol();
	
	Connector getConnector();
	
}