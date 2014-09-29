package com.partsoft.umsp.smgp;

import com.partsoft.umsp.smgp.Constants.RequestIDs;

public class ActiveTestResponse extends SmgpDataPacket {
	
	private static final long serialVersionUID = 0x80000004L;
	
	public ActiveTestResponse() {
		super(RequestIDs.active_test_resp);
	}
	
	@Override
	public ActiveTestResponse clone() {
		return (ActiveTestResponse) super.clone();
	}

}
