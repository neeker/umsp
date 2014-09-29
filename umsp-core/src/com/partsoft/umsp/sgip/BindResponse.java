package com.partsoft.umsp.sgip;

import com.partsoft.umsp.sgip.Constants.Commands;

public class BindResponse extends ResponsePacket {
	
	private static final long serialVersionUID = 0x80000001L;

	public BindResponse() {
		super(Commands.BIND_RESPONSE);
	}

}
