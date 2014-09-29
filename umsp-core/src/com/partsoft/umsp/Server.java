package com.partsoft.umsp;

import com.partsoft.umsp.handler.AbstractOriginHandler;
import com.partsoft.umsp.packet.PacketServerConnector;

public final class Server extends AbstractOriginHandler implements Attributes, OriginHandler {

	public Server() {
		setOrigin(this);
	}

	public Server(int port) {
		this("UNKNOWN", port);
	}

	public Server(String protocol, int port) {
		setOrigin(this);
		Connector connector = new PacketServerConnector(protocol);
		connector.setPort(port);
		setConnectors(new Connector[] { connector });
	}

}
