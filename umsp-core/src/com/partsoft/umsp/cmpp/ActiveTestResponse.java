package com.partsoft.umsp.cmpp;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.partsoft.umsp.cmpp.Constants.Commands;

public class ActiveTestResponse extends CmppDataPacket {
	
	private static final long serialVersionUID = 0x80000008L;

	protected ActiveTestResponse() {
		super(Commands.CMPP_ACTIVE_TEST_RESP);
	}

	@Override
	public ActiveTestResponse clone() {
		return (ActiveTestResponse) super.clone();
	}
	
	@Override
	protected void writeDataOutput(DataOutput out) throws IOException {
		super.writeDataOutput(out);
		out.writeByte(0);
	}
	
	@Override
	protected void readDataInput(DataInput in) throws IOException {
		super.readDataInput(in);
		in.readByte();
	}
	
	@Override
	public int getDataSize() {
		return super.getDataSize() + 1;
	}
	
}
