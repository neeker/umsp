package com.partsoft.umsp.sgip;

import com.partsoft.umsp.packet.PacketException;

public class SgipException extends PacketException {

	private static final long serialVersionUID = 8577738046412569758L;

	public SgipException(int size, int position, int offset) {
		super(size, position, offset);
	}

	public SgipException(int size, int offset) {
		super(size, offset);
	}

	public SgipException(String reason, Throwable e) {
		super(reason, e);
	}

	public SgipException(String reason) {
		super(reason);
	}

	public SgipException(Throwable e) {
		super(e);
	}
	
}
