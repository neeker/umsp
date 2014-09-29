package com.partsoft.umsp.sgip;

import com.partsoft.umsp.sgip.Constants.Commands;

public class UnBindResponse extends ResponsePacket {
	
	private static final long serialVersionUID = 0x80000002L;

	public UnBindResponse() {
		super(Commands.UNBIND_RESPONSE);
	}

}
