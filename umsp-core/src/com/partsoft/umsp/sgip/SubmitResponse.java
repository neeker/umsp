package com.partsoft.umsp.sgip;

import com.partsoft.umsp.sgip.Constants.Commands;

public class SubmitResponse extends ResponsePacket {
	
	private static final long serialVersionUID = 0x80000003L;

	public SubmitResponse() {
		super(Commands.SUBMIT_RESPONSE);
	}

}
