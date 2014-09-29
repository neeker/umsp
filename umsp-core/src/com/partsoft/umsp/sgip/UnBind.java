package com.partsoft.umsp.sgip;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.partsoft.umsp.sgip.Constants.Commands;

public class UnBind extends SgipDataPacket {
	
	private static final long serialVersionUID = 2L;
	
	public UnBind() {
		super(Commands.UNBIND);
	}

	@Override
	public UnBind clone() {
		return (UnBind) super.clone();
	}
	
	@Override
	protected void writeDataOutput(DataOutput out) throws IOException {
		super.writeDataOutput(out);
	}
	
	@Override
	protected void readDataInput(DataInput in) throws IOException {
		super.readDataInput(in);
	}
	
	@Override
	public int getDataSize() {
		return PACKET_SIZE - RESERVE_SIZE;
	}

}
