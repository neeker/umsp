package com.partsoft.umsp.cmpp;

import com.partsoft.umsp.cmpp.Constants.Commands;

public class Terminate extends CmppDataPacket {
	
	private static final long serialVersionUID = 2L;
	
	public Terminate() {
		super(Commands.CMPP_TERMINATE);
	}
	
	@Override
	public Terminate clone() {
		return (Terminate) super.clone();
	}
	
}
