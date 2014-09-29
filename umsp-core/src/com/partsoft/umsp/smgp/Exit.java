package com.partsoft.umsp.smgp;

import com.partsoft.umsp.smgp.Constants.RequestIDs;

public class Exit extends SmgpDataPacket {
	
	private static final long serialVersionUID = 6L;
	
	public Exit() {
		super(RequestIDs.exit);
	}

	@Override
	public Exit clone() {
		return (Exit) super.clone();
	}
	
}
