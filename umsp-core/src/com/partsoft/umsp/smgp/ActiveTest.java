package com.partsoft.umsp.smgp;

import com.partsoft.umsp.smgp.Constants.RequestIDs;

public class ActiveTest extends SmgpDataPacket {
	
	private static final long serialVersionUID = 4L;
	
	public ActiveTest() {
		super(RequestIDs.active_test);
	}
	
	@Override
	public ActiveTest clone() {
		return (ActiveTest) super.clone();
	}

}
