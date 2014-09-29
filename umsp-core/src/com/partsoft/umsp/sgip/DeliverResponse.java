package com.partsoft.umsp.sgip;

import com.partsoft.umsp.sgip.Constants.Commands;

public class DeliverResponse extends ResponsePacket {
	
	private static final long serialVersionUID = 0x80000004L;

	public DeliverResponse() {
		super(Commands.DELIVER_RESPONSE);
	}

	@Override
	public DeliverResponse clone() {
		return (DeliverResponse) super.clone();
	}

}
