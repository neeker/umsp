package com.partsoft.umsp.cmpp;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.partsoft.umsp.cmpp.Constants.Commands;

public class Connect extends CmppDataPacket {
	
	private static final long serialVersionUID = 1L;

	public String enterpriseId;
	
	public String authenticationToken;
	
	public int timestamp;
	
	public Connect() {
		super(Commands.CMPP_CONNECT);
	}
	
	@Override
	protected void writeDataOutput(DataOutput out) throws IOException {
		super.writeDataOutput(out);
		writeFixedString(out, enterpriseId, 6);
		writeFixedString(out, authenticationToken, 16);
		out.writeByte(protocolVersion);
		out.writeInt(timestamp);
	}
	
	@Override
	protected void readDataInput(DataInput in) throws IOException {
		super.readDataInput(in);
		enterpriseId = readFixedString(in, 6);
		authenticationToken = readFixedString(in, 16);
		protocolVersion = in.readUnsignedByte();
		timestamp = in.readInt();
	}
	
	@Override
	public int getDataSize() {
		return super.getDataSize() + 27;
	}
	
	@Override
	public Connect clone() {
		return (Connect) super.clone();
	}
	
}
