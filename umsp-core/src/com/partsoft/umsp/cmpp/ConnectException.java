package com.partsoft.umsp.cmpp;

public class ConnectException extends CmppException {

	private static final long serialVersionUID = 4531875680564123334L;

	public ConnectException(int size, int position, int offset) {
		super(size, position, offset);
	}

	public ConnectException(int size, int offset) {
		super(size, offset);
	}

	public ConnectException(String reason, Throwable e) {
		super(reason, e);
	}

	public ConnectException(String reason) {
		super(reason);
	}

	public ConnectException(Throwable e) {
		super(e);
	}
	
}
