package com.partsoft.umsp.packet;

import java.io.IOException;
import java.io.OutputStream;

import com.partsoft.umsp.Response;

public class PacketResponse implements Response {

	private PacketConnection _connection;

	protected void recycle() {
		
	}

	public PacketResponse(PacketConnection connection) {
		this._connection = connection;
	}

	public OutputStream getOutputStream() throws IOException {
		return _connection.getOutputStream();
	}

	public void complete() throws IOException {
		_connection.completeResponse();
	}

	public void setContentLength(int paramInt) {
		_connection.getGenerator().setContentLength(paramInt);
	}

	public void setBufferSize(int paramInt) {
		if (isCommitted())
			return;
		_connection.getGenerator().setContentLength(paramInt);
	}

	public int getBufferSize() {
		return _connection.getGenerator().getContentBufferSize();
	}

	public void flushBuffer() throws IOException {
		_connection.flushResponse();
	}

	public void resetBuffer() {
		if (isCommitted())
			throw new IllegalStateException("Committed");
		_connection.getGenerator().resetBuffer();
	}

	public boolean isCommitted() {
		return _connection.isResponseCommitted();
	}

	public void reset() {
		resetBuffer();
	}

	public void finalComplete() throws IOException {
		_connection.finalCompleteResponse();
	}

	public void errorTerminated() throws IOException {
		_connection.terminateResponse();
	}

	public void finalBuffer() throws IOException {
		finalComplete();
	}
	
}
