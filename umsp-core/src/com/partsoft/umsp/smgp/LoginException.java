package com.partsoft.umsp.smgp;

public class LoginException extends SmgpException {

	private static final long serialVersionUID = 4531875680564123334L;

	public LoginException(int size, int position, int offset) {
		super(size, position, offset);
	}

	public LoginException(int size, int offset) {
		super(size, offset);
	}

	public LoginException(String reason, Throwable e) {
		super(reason, e);
	}

	public LoginException(String reason) {
		super(reason);
	}

	public LoginException(Throwable e) {
		super(e);
	}
	
}
