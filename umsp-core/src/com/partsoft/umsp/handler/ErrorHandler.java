package com.partsoft.umsp.handler;

import java.io.IOException;

import com.partsoft.umsp.Request;
import com.partsoft.umsp.Response;
import com.partsoft.umsp.packet.PacketConnection;

public class ErrorHandler extends AbstractHandler {

	boolean _showStacks = true;

	public void handle(String protocol, Request request, Response response, int dispatch) throws IOException {
		handleError(protocol, request, response, dispatch);
	}

	protected void handleError(String protocol, Request request, Response response, int dispatch) throws IOException {
		PacketConnection conn = PacketConnection.getCurrentConnection();
		conn.getRequest().setHandled(true);
		conn.terminateResponse();
	}

}
