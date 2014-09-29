package com.partsoft.umsp.sgip;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.partsoft.umsp.io.Buffer;
import com.partsoft.umsp.sgip.Constants.BindResults;

public class ResponsePacket extends SgipDataPacket {

	private static final long serialVersionUID = 4279081553535071020L;

	public static final int PACKET_SIZE = SgipDataPacket.PACKET_SIZE + Buffer.BYTE_SIZE;

	public byte result = BindResults.ERROR;

	public ResponsePacket(int command) {
		super(command);
	}

	@Override
	protected void writeDataOutput(DataOutput out) throws IOException {
		super.writeDataOutput(out);
		out.writeByte(result);
		writeFixedString(out, reserve, RESERVE_SIZE);
	}

	@Override
	protected void readDataInput(DataInput in) throws IOException {
		super.readDataInput(in);
		result = in.readByte();
		reserve = readFixedString(in, RESERVE_SIZE);
	}

	@Override
	public String toString() {
		return "联通SGIP应答数据包 [应答状态=" + result + "]";
	}

	@Override
	public int getDataSize() {
		return PACKET_SIZE;
	}

}
