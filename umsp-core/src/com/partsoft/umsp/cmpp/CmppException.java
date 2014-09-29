package com.partsoft.umsp.cmpp;

import com.partsoft.umsp.packet.PacketException;

public class CmppException extends PacketException {

	private static final long serialVersionUID = 8577738046412569758L;

	public CmppException(int size, int position, int offset) {
		super(size, position, offset);
	}

	public CmppException(int size, int offset) {
		super(size, offset);
	}

	public CmppException(String reason, Throwable e) {
		super(reason, e);
	}

	public CmppException(String reason) {
		super(reason);
	}

	public CmppException(Throwable e) {
		super(e);
	}
	
}
