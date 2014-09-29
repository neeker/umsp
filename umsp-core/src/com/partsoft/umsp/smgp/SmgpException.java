package com.partsoft.umsp.smgp;

import com.partsoft.umsp.packet.PacketException;

public class SmgpException extends PacketException {

	private static final long serialVersionUID = 8577738046412569758L;

	public SmgpException(int size, int position, int offset) {
		super(size, position, offset);
	}

	public SmgpException(int size, int offset) {
		super(size, offset);
	}

	public SmgpException(String reason, Throwable e) {
		super(reason, e);
	}

	public SmgpException(String reason) {
		super(reason);
	}

	public SmgpException(Throwable e) {
		super(e);
	}
	
}
