package com.partsoft.umsp.sgip;

public class BindException extends SgipException {

	private static final long serialVersionUID = 4531875680564123334L;

	public BindException(int size, int position, int offset) {
		super(size, position, offset);
	}

	public BindException(int size, int offset) {
		super(size, offset);
	}

	public BindException(String reason, Throwable e) {
		super(reason, e);
	}

	public BindException(String reason) {
		super(reason);
	}

	public BindException(Throwable e) {
		super(e);
	}
	
}
