package com.partsoft.umsp.io;

import java.io.EOFException;

public class EofException extends EOFException {
	
	private static final long serialVersionUID = 5108545487073551382L;

	public EofException() {
	}

	public EofException(String reason) {
		super(reason);
	}

	public EofException(Throwable th) {
		initCause(th);
	}
}
