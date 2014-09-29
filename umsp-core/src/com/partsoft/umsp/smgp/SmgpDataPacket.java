package com.partsoft.umsp.smgp;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.partsoft.umsp.DataPacket;
import com.partsoft.umsp.io.Buffer;
import com.partsoft.umsp.packet.AbstractDataPacket;
import com.partsoft.utils.Assert;

public abstract class SmgpDataPacket extends AbstractDataPacket implements DataPacket, Cloneable {

	private static final long serialVersionUID = 6148118459292364553L;

	protected static final int PACKET_HEADER_SIZE = 8;

	public final int requestId;

	public int sequenceId;

	public boolean enabled = true;

	protected SmgpDataPacket(int command) {
		this.requestId = command;
	}

	public int getRequestId() {
		return requestId;
	}

	protected void writeDataOutput(DataOutput out) throws IOException {
		if (out == null)
			new IOException();
		out.writeInt(requestId);
		out.writeInt(sequenceId);
	}

	protected void readDataInput(DataInput in) throws IOException {
		if (in == null)
			new IOException();
		int tmp_requestid = in.readInt();
		Assert.isTrue(tmp_requestid == this.requestId);
		sequenceId = in.readInt();
	}

	public int getDataSize() {
		return PACKET_HEADER_SIZE;
	}

	public int getBufferSize() {
		return getDataSize() + Buffer.INT_SIZE;
	}

	public SmgpDataPacket clone() {
		SmgpDataPacket new_obj = (SmgpDataPacket) super.clone();
		return new_obj;
	}
	
	public int getCommandID() {
		return this.requestId;
	}

	@Override
	public String toString() {
		return "电信SMGP协议包 [指令=" + requestId + ", 序号=" + sequenceId + ", 创建时间=" + getCreateTimeMillis() + "]";
	}

}
