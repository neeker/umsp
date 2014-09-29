package com.partsoft.umsp.cmpp;

import com.partsoft.umsp.cmpp.Constants.Commands;

public class TerminateResponse extends CmppDataPacket {
	
	private static final long serialVersionUID = 0x80000002L;
	
	public TerminateResponse() {
		super(Commands.CMPP_TERMINATE_RESP);
	}
	
	@Override
	public TerminateResponse clone() {
		return (TerminateResponse) super.clone();
	}
	
}
