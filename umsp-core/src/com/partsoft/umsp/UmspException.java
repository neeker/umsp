package com.partsoft.umsp;

import java.io.Serializable;

public class UmspException extends RuntimeException implements Serializable {

	private static final long serialVersionUID = -6879583987345986122L;
	
	private int errorCode = -1;

	public UmspException(String reason) {
		super(reason);
	}

	public UmspException(int errorCode, String reason) {
		super(reason);
		this.errorCode = errorCode;
	}
	
	public UmspException(int errorCode, Throwable e) {
		super(e);
		this.errorCode = errorCode;
	}
	
	public UmspException(String reason, Throwable e) {
		super(reason, e);
	}
	
	public UmspException() {
		super();
	}

	public UmspException(Throwable cause) {
		super(cause);
	}

	public void finalize() {
		try {
			super.finalize();
		} catch (Throwable e) {
			
		}
	}

	public int getErrorCode() {
		return errorCode;
	}

	public Throwable getLinkedException() {
		return getCause();
	}
}