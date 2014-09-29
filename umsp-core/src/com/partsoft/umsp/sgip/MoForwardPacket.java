package com.partsoft.umsp.sgip;

public abstract class MoForwardPacket extends SgipDataPacket {

	private static final long serialVersionUID = 6787408637498157588L;
	
	//提交次数
	public int submitCount;
	
	protected MoForwardPacket(int command) {
		super(command);
	}

}
